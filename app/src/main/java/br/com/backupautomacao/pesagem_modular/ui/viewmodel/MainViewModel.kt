package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import br.com.backupautomacao.bluetooth.BluetoothConnected
import br.com.backupautomacao.bluetooth.BluetoothHandler
import br.com.backupautomacao.bluetooth.exceptions.ClosedSocketException
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.db.model.Operation
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository.Companion.PRINTER_IP
import br.com.backupautomacao.pesagem_modular.db.repository.OperationRepository
import br.com.backupautomacao.pesagem_modular.network.ApiPesagem
import br.com.backupautomacao.pesagem_modular.network.MgvProduct
import br.com.backupautomacao.pesagem_modular.printers.Epson
import br.com.backupautomacao.pesagem_modular.printers.HoneywellPrinter
import br.com.backupautomacao.pesagem_modular.utils.Formatter
import br.com.backupautomacao.pesagem_modular.utils.Formatter.Companion.removeNonAlphaCharacters
import br.com.backupautomacao.pesagem_modular.utils.Label
import br.com.backupautomacao.pesagem_modular.utils.PrinterType
import br.com.backupautomacao.pesagem_modular.utils.ResourceProvider
import br.com.backupautomacao.pesagem_modular.utils.ScaleStatus
import br.com.backupautomacao.pesagem_modular.utils.StatusApp
import br.com.backupautomacao.pesagem_modular.utils.UnitOfMeasure
import br.com.backupautomacao.pesagem_modular.utils.UsbPrinterModel
import br.com.backupautomacao.pesagem_modular.utils.Util
import br.com.backupautomacao.pesagem_modular.utils.findItem
import br.com.backupautomacao.pesagem_modular.workers.NetworkWorker
import br.com.backupautomacao.pesagem_modular.workers.PRINT_TASK
import br.com.backupautomacao.pesagem_modular.workers.UsbWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.gotev.speech.SpeechDelegate
import timber.log.Timber
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.security.InvalidParameterException

class MainViewModel(
  private val epson: Epson,
  private val honeywellPrinter: HoneywellPrinter,
  private val configRepository: ConfigRepository,
  private val operationRepository: OperationRepository,
  private val workManager: WorkManager,
  private val bluetoothHandler: BluetoothHandler,
  private val resourceProvider: ResourceProvider,
  private val label: Label,
  private val apiPesagem: ApiPesagem
) : ViewModel(), SpeechDelegate {

  private val _showQuantity = MutableStateFlow(false)
  val showQuantity = _showQuantity.asStateFlow()

  val quantity = MutableLiveData("1")

  private val _mgvProduct = MutableLiveData<MgvProduct?>(null)
  val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

  private val _statusMessage = MutableSharedFlow<String>()
  val statusMessage = _statusMessage.asSharedFlow()

  private val _scale = MutableStateFlow<Int?>(null)
  val scale = _scale.asStateFlow()

  private val _usbDevice = MutableStateFlow<UsbDevice?>(null)
  private val _updateStatus = MutableStateFlow(StatusApp.INICIO)

  private val _devices = MutableStateFlow<List<BluetoothConnected>>(listOf())
  val devices = _devices.asStateFlow()

  private val _activeDevice = MutableStateFlow<BluetoothConnected?>(null)
  val activeDevice = _activeDevice.asStateFlow()

  private val _isActiveTare = MutableStateFlow(false)
  val isActiveTare = _isActiveTare.asStateFlow()

  private val _currentTare = MutableStateFlow<Double?>(null)
  val currentTare = _currentTare.asStateFlow()

  private val weightReadyChannel = Channel<Double>(Channel.UNLIMITED)

  val automaticPrintingEnable: Boolean
    get() = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)

  private val jobsRunning = mutableListOf<Job>()

  private val _isFixedProduct = MutableStateFlow(false)
  val fixedProduct = _isFixedProduct.asStateFlow()

  init {
    applicationMonitor()
    initializePrinter()
  }

  private fun initializePrinter() {
    val config = configRepository.getUsbConfig()
      ?: return emitMessage("Nenhuma configuração USB definida")

    val parts = config.split(";")
    if (parts.size < 2) {
      return emitMessage("Configuração USB inválida")
    }

    val (model, deviceName) = parts
    val usbDevice = try {
      honeywellPrinter.device(deviceName)
    } catch (e: Exception) {
      return emitMessage("Dispositivo USB '$deviceName' não encontrado")
    }

    if (model.contains("TM-L100", ignoreCase = true)) {
      epson.initializePrinter(config)
    } else {
      honeywellPrinter.initializePrinter(usbDevice)
    }
  }

  fun toggleShowQuantity() {
    _showQuantity.update { !_showQuantity.value }
  }

  fun setProduct(product: MgvProduct) {
    _mgvProduct.value = product
    quantity.value = product.quantity?.toString() ?: "1"
    _showQuantity.value = product.unitOfMeasure == UnitOfMeasure.UNIDADE
  }


  fun updateProductQuantity(newQuantityStr: String) {

    val newQuantity = newQuantityStr.toIntOrNull()

    if (newQuantity == null) {
      emitMessage("Quantidade inválida")
      return
    }

    _mgvProduct.value?.let { product ->
      val updatedProduct = product.copy(
        quantity = newQuantity,
        totalPrice = product.price * newQuantity
      )
      _mgvProduct.value = updatedProduct
      emitMessage("Quantidade atualizada: $newQuantity")
      loadPrice(updatedProduct.totalPrice ?: 0.0)
    }
  }

  private fun applicationMonitor() {
    _updateStatus.onEach { status ->
      Timber.i("applicationMonitor: Status atualizado para $status")

      val automaticPrint = configRepository
        .getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
      Timber.i("AUTOMATIC_PRINT está %s", automaticPrint)
      Timber.i("applicationMonitor: AUTOMATIC_PRINT flag está em ${if (automaticPrint) "ATIVADO" else "DESATIVADO"}")

      when (status) {
        StatusApp.INICIO -> Timber.i("applicationMonitor: INICIO")
        StatusApp.BUSCAR_PRODUTO -> Timber.i("applicationMonitor: BUSCAR_PRODUTO")
        StatusApp.REALIZAR_PESAGEM -> {
          Timber.i("applicationMonitor: REALIZAR_PESAGEM")
          activeDevice.value?.let { weighining(it) }
        }

        StatusApp.GRAVAR_OPERACAO -> {
          Timber.i("applicationMonitor: GRAVAR_OPERACAO")
          saveOperation()
        }

        StatusApp.REALIZAR_IMPRESSAO -> {
          Timber.i("applicationMonitor: REALIZAR_IMPRESSAO")
          if (automaticPrint) print()
        }

        else -> Timber.i("applicationMonitor: Processo finalizado")
      }
    }.launchIn(viewModelScope)
  }

  fun getUsbConfig(): String? {
    return configRepository.getUsbConfig()
  }

  fun getCurrentProduct(): MgvProduct? {
    return mgvProduct.value
  }

  fun requestPermissionUSB(context: Context) {
    _usbDevice.value?.let { epson.requestPermission(it, context) }
  }

  fun cancelPrint() {
    workManager.cancelUniqueWork(PRINT_TASK)
  }

  private suspend fun getWeight(btConnected: BluetoothConnected): Double {
    val command = byteArrayOf(0x05)
    btConnected.write(command)

    //Limpa o canal de qualquer valor previamente utilizado
    weightReadyChannel.tryReceive().getOrNull()

    //Faz a busca do peso na balança
    val newWeight = withTimeoutOrNull(3000L) {
      weightReadyChannel.receive()
    }

    if (newWeight == null) {
      throw Exception(resourceProvider.getString(R.string.weight_timeout_exceed))
    }

    return newWeight
  }

  suspend fun weighining(btConnected: BluetoothConnected?) {
    Timber.i("weighing: Realizando pesagem")

    if (btConnected == null) {
      showMessage("Balança não pareada")
      return
    }

    if (checkProductIsNullOrEmpty()) {
      showMessage(resourceProvider.getString(R.string.error_get_product))
      return
    }

    try {
      loadTare(btConnected, _currentTare.value ?: 0.0)
      delay(500)
      val newWeight = getWeight(btConnected)
      val currentProduct = _mgvProduct.value
      val currentTare = _currentTare.value
      val updatedProduct = currentProduct?.copy(
        tare = currentTare ?: 0.0,
        weight = newWeight,
        totalPrice = currentProduct.price.times(newWeight)
      )
      currentProduct?.let { loadPrice(it.price) }
      _mgvProduct.value = updatedProduct
      Timber.i("Produto: ${_mgvProduct.value}")
      _updateStatus.update { StatusApp.GRAVAR_OPERACAO }
    } catch (e: ClosedSocketException) {
      e.message?.let { emitMessage(it) }
    } catch (e: Exception) {
      e.message?.let { emitMessage(it) }
    }
  }

  suspend fun changeTare(btConnected: BluetoothConnected) {
    try {
      _isActiveTare.value = !_isActiveTare.value
      val weight = if (_isActiveTare.value) {
        getWeight(btConnected)
      } else {
        0.0
      }
      Timber.i("Peso para tarar: $weight")
      val tareBytes = Formatter.formatDataToLoadTareToledo(weight)
      btConnected.write(tareBytes)
      _currentTare.update { weight }
    } catch (e: ClosedSocketException) {
      e.message?.let { emitMessage(it) }
    } catch (e: Exception) {
      e.message?.let { emitMessage(it) }
    }
  }

  private fun loadTare(btConnected: BluetoothConnected?, value: Double) {
    if (btConnected == null) {
      showMessage("Balança não pareada")
      return
    }

    val newTare = Formatter.formatDataToLoadTareToledo(value)
    btConnected.write(newTare)
  }

  private fun startCollectData(device: BluetoothConnected) {

    val job = device.read().onEach { bytes ->
      val rawData = String(bytes, StandardCharsets.UTF_8)
      Timber.i("Peso: ${String(bytes, StandardCharsets.UTF_8)}")

      if (rawData.isEmpty()) {
        return@onEach
      }

      val clean = removeNonAlphaCharacters(rawData)
      Timber.i("Peso: '$rawData' | Limpo: '$clean'")

      when (removeNonAlphaCharacters(rawData)) {
        ScaleStatus.UNSTABLE.value -> emitMessage("Peso instável, faça a pesagem novamente")
        ScaleStatus.NEGATIVE_WEIGHT.value -> emitMessage("Balança com peso negativo")
        ScaleStatus.OVERWEIGHT.value -> emitMessage("Balança em sobrecarga, remove o peso extra")
        else -> updateWeight(bytes)
      }

    }.catch { e ->
      e.message?.let { showMessage(it) }
    }.launchIn(viewModelScope)

    jobsRunning.add(job)
  }

  private fun cancelAllJobs() {
    jobsRunning.forEach { job ->
      job.cancel()
    }
  }

  override fun onCleared() {
    super.onCleared()
    cancelAllJobs()
    weightReadyChannel.close()
    epson.finalizePrinter()
  }

  suspend fun connectToDevices(btAdapter: BluetoothAdapter) {
    val devices = bluetoothHandler.getAvailableDevices(btAdapter)
    val newDevices = mutableListOf<BluetoothConnected>()
    emitMessage("Buscando dispositivos disponíveis")
    devices.forEach { device ->

      val socket = withContext(Dispatchers.IO) {
        bluetoothHandler.connectToDevice(device)
      }

      emitMessage("Conectando ao dispositivo ${newDevices.size + 1}")
      newDevices.add(socket)
      startCollectData(socket)
      emitMessage("Dispositivo Conectado")

      if (newDevices.size == 1) {
        _activeDevice.update { newDevices[0] }
      }
      _devices.update { newDevices.toList() }
    }
  }

  fun emitMessage(text: String) {
    viewModelScope.launch {
      _statusMessage.emit(text)
    }
  }

  val statusPrint = workManager.getWorkInfosForUniqueWorkFlow(PRINT_TASK)

  private fun updateWeight(message: ByteArray) {
    val newWeight: Double = Formatter.formatFromToledoProtocolP05(message)
    weightReadyChannel.trySend(newWeight)
  }

  fun setActiveDevice(btConnected: BluetoothConnected) {
    _activeDevice.update { btConnected }
  }

  fun fetchItem(codItem: String) {
    Timber.i("fetchItem: Buscando item")

    _updateStatus.update { StatusApp.BUSCAR_PRODUTO }

    viewModelScope.launch {
      try {
        val product = withContext(Dispatchers.IO) {
          apiPesagem.getProduct(codItem)
        }

        val unitCode = product.unitOfMeasure
        Timber.i("fetchItem: Produto recebido - Código: ${product.code}, UnitOfMeasureInt: $unitCode")

        val isQuantity = unitCode == UnitOfMeasure.UNIDADE
        _showQuantity.update { isQuantity }

        val updatedProduct = if (isQuantity) {
          val quantity = product.quantity ?: 1
          val totalPrice = product.price * quantity
          product.copy(
            quantity = quantity,
            totalPrice = totalPrice
          )
        } else {
          product
        }

        _mgvProduct.value = updatedProduct

        if (isQuantity) {
          quantity.value = updatedProduct.quantity.toString()
          loadPrice(updatedProduct.totalPrice ?: 0.0)
        }

        when (unitCode) {
          UnitOfMeasure.PESO -> {
            Timber.i("fetchItem: Produto por peso - indo para REALIZAR_PESAGEM")
            _updateStatus.update { StatusApp.REALIZAR_PESAGEM }
          }

          UnitOfMeasure.UNIDADE -> {
            Timber.i("fetchItem: Produto por unidade - indo para GRAVAR_OPERACAO")
            _updateStatus.update { StatusApp.GRAVAR_OPERACAO }
          }

          else -> {
            Timber.w("fetchItem: Produto sem unidade definida! Usando GRAVAR_OPERACAO como fallback.")
            _updateStatus.update { StatusApp.GRAVAR_OPERACAO }
          }
        }

      } catch (e: Exception) {
        e.printStackTrace()
        showMessage(e.message ?: "Ocorreu um erro")
      }
    }
  }


  /*
  fun fetchItem(codItem: String) {
    Timber.i("fetchItem: Buscando item")

    _updateStatus.update { StatusApp.BUSCAR_PRODUTO }

    viewModelScope.launch {
      try {
        val product = withContext(Dispatchers.IO) {
          apiPesagem.getProduct(codItem)
        }

        _mgvProduct.value = product

        val unitCode = product.unitOfMeasure
        Timber.i("fetchItem: Produto recebido - Código: ${product.code}, UnitOfMeasureInt: $unitCode")
        val isQuantity = unitCode == UnitOfMeasure.UNIDADE
        Timber.i("É quantidade? $isQuantity")
        _showQuantity.update { isQuantity }

        when (unitCode) {
          UnitOfMeasure.PESO -> {
            Timber.i("fetchItem: Produto por peso - indo para REALIZAR_PESAGEM")
            _updateStatus.update { StatusApp.REALIZAR_PESAGEM }
          }

          UnitOfMeasure.UNIDADE -> {
            Timber.i("fetchItem: Produto por unidade - indo para GRAVAR_OPERACAO")
            _updateStatus.update { StatusApp.GRAVAR_OPERACAO }
          }

          else -> {
            Timber.w("fetchItem: Produto sem unidade definida! Usando GRAVAR_OPERACAO como fallback.")
            _updateStatus.update { StatusApp.GRAVAR_OPERACAO }
          }
        }

      } catch (e: Exception) {
        e.printStackTrace()
        showMessage(e.message ?: "Ocorreu um erro")
      }
    }
  }

   */

  fun print() {
    if (checkProductIsNullOrEmpty()) {
      showMessage(resourceProvider.getString(R.string.error_to_get_product))
      return
    }

    val product = _mgvProduct.value
    if (product == null) {
      showMessage(resourceProvider.getString(R.string.error_to_get_product))
      return
    }

    val isPrintingByQuantity = _showQuantity.value == true

    if (!isPrintingByQuantity) {
      if (product.weight == null || product.weight == 0.0) {
        showMessage(resourceProvider.getString(R.string.make_weighing))
        return
      }
    }

    try {
      val printerType = configRepository.getPrinterType()
      Timber.i("Impressora selecionada: $printerType")

      when (printerType) {
        PrinterType.USB -> {
          val savedDeviceName = configRepository.getUsbConfig()

          if (savedDeviceName == null) {
            showMessage("Nenhuma impressora USB configurada.")
            return
          }

          val printerModelEnum = configRepository.getUsbPrinterModel()
          when (printerModelEnum) {
            UsbPrinterModel.EPSON -> {
              epsonPrint(savedDeviceName, product)
            }

            UsbPrinterModel.HONEYWELL -> {
              viewModelScope.launch {
                honeywellPrint(savedDeviceName, product, isPrintingByQuantity)
              }
            }
          }

          if (!_isFixedProduct.value) {
            clearFields()
          } else {
            _mgvProduct.value = _mgvProduct.value?.copy(
              weight = null,
              totalPrice = null
            )
          }

          _updateStatus.update { StatusApp.FINALIZAR }

        }

        PrinterType.NETWORK -> {
          val zpl = label.generateLabelBase(product, null)
          createNetworkTask(zpl)

          _scale.value = null
          _updateStatus.update { StatusApp.FINALIZAR }
          clearFields()
        }

      }

    } catch (e: FileNotFoundException) {
      showMessage(resourceProvider.getString(R.string.file_not_found_error))
    } catch (e: InvalidParameterException) {
      Timber.e(e)
      showMessage(resourceProvider.getString(R.string.invalid_printer_config))
    } catch (e: Exception) {
      Timber.e(e)
      showMessage("${resourceProvider.getString(R.string.unexpected_error)}: ${e.localizedMessage}")
    }
  }

  fun changeProductState() {
    _isFixedProduct.update { !_isFixedProduct.value }
  }

  private suspend fun honeywellPrint(
    deviceName: String,
    product: MgvProduct,
    isQuantityPrinting: Boolean
  ) {
    val device = honeywellPrinter.device(deviceName)
    honeywellPrinter.initializePrinter(device)

    val success = withContext(Dispatchers.IO) {
      honeywellPrinter.safeConnectSuspend()
    }

    if (!success) {
      showMessage("Falha na conexão com a impressora Honeywell")
      return
    }

    withContext(Dispatchers.IO) {
      try {
        honeywellPrinter.print(product, isQuantityPrinting)
      } catch (e: Exception) {
        e.message?.let { showMessage(it) }
      }
    }
  }

  private fun epsonPrint(deviceName: String, product: MgvProduct) {
    epson.initializePrinter(deviceName)
    val created = epson.createReceiptData(product)
    if (created) {
      val success = epson.printData()
      if (!success) {
        showMessage("Erro ao imprimir na Epson")
      }
    } else {
      showMessage("Erro na definição Epson")
    }
  }

  private fun createNetworkTask(label: String): OneTimeWorkRequest {
    val printerIp = configRepository.getConfig(PRINTER_IP)
    val printerPort = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    return NetworkWorker.createUniquePrintTask(
      printerAddress = printerIp,
      printerPort = printerPort,
      label = label,
    )
  }

  private fun createUsbTask(label: String): OneTimeWorkRequest {
    val usbPrinterName = configRepository.getUsbConfig()
    return UsbWorker.createUniquePrintTask(
      usbPrinterName = usbPrinterName,
      label = label
    )
  }

  private fun checkProductIsNullOrEmpty(): Boolean {
    return _mgvProduct.value?.run {
      code.isEmpty()
    } != false
  }

  fun showMessage(message: String = "") {
    emitMessage(message)
  }

  private fun loadPrice(value: Double) {
    val priceByteArray = Formatter.formatDataToLoadPriceToledo(value)
    _activeDevice.value?.write(priceByteArray)
  }

  private fun clearFields() {
    _mgvProduct.value = MgvProduct()
  }

  private fun saveOperation() {
    try {
      val product = _mgvProduct.value
        ?: throw Exception(resourceProvider.getString(R.string.save_operation_error))

      val operation = when (product.unitOfMeasure) {
        UnitOfMeasure.UNIDADE -> {
          val quantity = product.quantity?.toDouble() ?: 1.0
          Operation(
            weight = 0.0,
            totalPrice = product.totalPrice ?: (product.price * quantity),
            pricePerKilogram = 0.0,
            pricePerUnit = product.price,
            username = Build.ID,
            description = product.description.ifBlank { "Produto sem descrição" }
          )
        }

        UnitOfMeasure.PESO -> {
          val weight = product.weight
            ?: throw Exception("Peso não informado")
          val total = product.totalPrice
            ?: throw Exception("Preço total não informado")
          Operation(
            weight = weight,
            totalPrice = total,
            pricePerKilogram = product.price,
            pricePerUnit = 0.0,
            username = Build.ID,
            description = product.description.ifBlank { "Produto sem descrição" }
          )
        }

        else -> {
          throw Exception("Tipo de unidade inválido ou não definido")
        }
      }

      viewModelScope.launch {
        operationRepository.save(operation)
      }

      _updateStatus.value = StatusApp.REALIZAR_IMPRESSAO

    } catch (e: Exception) {
      Timber.e(e, "saveOperation: Erro ao salvar operação")
      showMessage(e.message ?: "Erro desconhecido")
    }
  }

  override fun onStartOfSpeech() {
    showMessage(resourceProvider.getString(R.string.listening))
  }

  override fun onSpeechRmsChanged(value: Float) {

  }

  override fun onSpeechPartialResults(results: MutableList<String>?) {

  }

  override fun onSpeechResult(result: String?) {
    //Balança 1 código 29 [balança, 1, código, 29]
    //Balança 1 bacon [balanca, 1, bacon]
    val wordsArray = result?.split(" ") ?: listOf()
    wordsArray.forEach {
      Timber.i("onSpeechResult: $it")
    }
    executeTask(wordsArray)
  }

  fun listening() {
    //speechInstance.startListening(this)
  }

  private fun executeTask(wordsArray: List<String>) {
    if (wordsArray.isNotEmpty()) {
      val (_, index) = wordsArray
      val product = wordsArray
        .drop(2)
        .joinToString { words -> words }
        .replace(",", "")
      val scaleNumber = Util.transformStringToNumber(index) - 1
      val code = findItem(product)
      Timber.i("executeTask: Balança: $scaleNumber")
      Timber.i("executeTask: Número Balança -> $index")
      Timber.i("executeTask: Produto -> $product")
      Timber.i("executeTask: Código -> $code")
      if (scaleNumber < 0) {
        showMessage(resourceProvider.getString(R.string.error_to_set_scale))
        return
      }
      if (code == 0) {
        showMessage(resourceProvider.getString(R.string.product_not_found))
        return
      }
      _scale.value = scaleNumber
      Timber.i("executeTask: Indice Balança ${_scale.value}")
      fetchItem(codItem = code.toString())
    }
  }
}