package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import br.com.backupautomacao.bluetooth.BluetoothConnected
import br.com.backupautomacao.bluetooth.BluetoothHandler
import br.com.backupautomacao.bluetooth.exceptions.ClosedSocketException
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.db.model.Operation
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository.Companion.PRINTER_IP
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import br.com.backupautomacao.centraldepesagem.network.ExtraInfo
import br.com.backupautomacao.centraldepesagem.network.MgvInformacoesNutricionais
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.network.MgvService
import br.com.backupautomacao.centraldepesagem.printers.Epson
import br.com.backupautomacao.centraldepesagem.printers.HoneywellPrinter
//import br.com.backupautomacao.centraldepesagem.printers.PrinterManager
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.removeNonAlphaCharacters
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider
import br.com.backupautomacao.centraldepesagem.utils.ScaleStatus
import br.com.backupautomacao.centraldepesagem.utils.StatusApp
import br.com.backupautomacao.centraldepesagem.utils.Util
import br.com.backupautomacao.centraldepesagem.utils.findItem
import br.com.backupautomacao.centraldepesagem.workers.NetworkWorker
import br.com.backupautomacao.centraldepesagem.workers.PRINT_TASK
import br.com.backupautomacao.centraldepesagem.workers.UsbWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.gotev.speech.SpeechDelegate
import timber.log.Timber
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.security.InvalidParameterException


class MainViewModel(
//  private val printerUsb: PrinterUsb,
  private val appContext: Context,
  private val epson: Epson,
  private val honeywellPrinter: HoneywellPrinter,
  private val configRepository: ConfigRepository,
  private val operationRepository: OperationRepository,
  private val workManager: WorkManager,
  private val bluetoothHandler: BluetoothHandler,
  private val resourceProvider: ResourceProvider,
  private val label: Label,
  private val mgvService: MgvService
) : ViewModel(), SpeechDelegate {

//  private lateinit var printerManager: PrinterManager
//private val printerManager: PrinterManager by lazy {
//  PrinterManager(appContext, configRepository)
//}

  private val _mgvProduct = MutableLiveData<MgvProduct?>()
  val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

  private val _statusMessage = MutableSharedFlow<String>()
  val statusMessage = _statusMessage.asSharedFlow()

  private val _scale: MutableStateFlow<Int?> = MutableStateFlow(null)
  val scale: StateFlow<Int?> = _scale

  private val _usbDevice: MutableStateFlow<UsbDevice?> = MutableStateFlow(null)
  private val _updateStatus: MutableStateFlow<StatusApp> = MutableStateFlow(StatusApp.INICIO)

  private val _devices = MutableStateFlow<List<BluetoothConnected>>(listOf())
  val devices = _devices.asStateFlow()

  private val _activeDevice = MutableStateFlow<BluetoothConnected?>(null)
  val activeDevice = _activeDevice.asStateFlow()

  private val jobsRunning = mutableListOf<Job>()


  fun initializePrinter() {
    applicationMonitor()

    val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    val config = configRepository.getUsbConfig()

    if (config == null) {
      viewModelScope.launch {
        emitMessage("Nenhuma configuração USB definida")
      }
      return
    }

    val parts = config.split(";")
    if (parts.size < 2) {
      viewModelScope.launch {
        emitMessage("Configuração USB inválida")
      }
      return
    }

    val model = parts[0]
    val deviceName = parts[1]

    val usbDevice = usbManager.deviceList.values.firstOrNull { it.deviceName == deviceName }

    if (usbDevice == null) {
      viewModelScope.launch {
        emitMessage("Dispositivo USB '$deviceName' não encontrado")
      }
      return
    }

    if (model.contains("TM-L100", ignoreCase = true)) {
      epson.initializePrinter(config)
    } else {
      honeywellPrinter.initializePrinter(usbDevice)
    }
  }


  //HONEYWELL
  /*
  fun initializePrinter() {
    applicationMonitor()

    val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    val deviceName = configRepository.getUsbConfig()

    if (deviceName == null) {
      viewModelScope.launch {
        emitMessage("Nenhuma configuração USB definida")
      }
      return
    }

    val usbDevice = usbManager.deviceList.values.firstOrNull { it.deviceName == deviceName }

    if (usbDevice == null) {
      viewModelScope.launch {
        emitMessage("Dispositivo USB '$deviceName' não encontrado")
      }
      return
    }

    honeywellPrinter.initializePrinter(usbDevice)
  }

   */

  private fun applicationMonitor() {
    _updateStatus.onEach { status ->
      val automaticPrint = configRepository
        .getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
      when (status) {
        StatusApp.INICIO -> Timber.i("Inicio: ")
        StatusApp.BUSCAR_PRODUTO -> Timber.i("Buscando produto")
        StatusApp.REALIZAR_PESAGEM -> activeDevice.value?.let { weighining(it) }
        StatusApp.GRAVAR_OPERACAO -> saveOperation()
        StatusApp.REALIZAR_IMPRESSAO -> if (automaticPrint) print()
        else -> Timber.i("Processo finalizado")
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

  fun weighining(btConnected: BluetoothConnected) {
    Timber.i("weighing: Realizando pesagem")
    if (checkProductIsNullOrEmpty()) {
      showMessage(resourceProvider.getString(R.string.error_get_product))
      return
    }
    try {
      val command = byteArrayOf(0x05)
      btConnected.write(command)
    } catch (e: ClosedSocketException) {
      e.message?.let { emitMessage(it) }
    } catch (e: Exception) {
      e.message?.let { emitMessage(it) }
    }
  }

  private fun startCollectData(device: BluetoothConnected) {

    val job = device.read().onEach { bytes ->
      val rawData = String(bytes, StandardCharsets.UTF_8)
      Timber.i("Peso: ${String(bytes, StandardCharsets.UTF_8)}")

      if (rawData.isEmpty()) {
        return@onEach
      }

      when (removeNonAlphaCharacters(rawData)) {
        ScaleStatus.UNSTABLE.value -> emitMessage("Peso instável, faça a pesagem novamente")
        ScaleStatus.NO_WEIGHT.value -> emitMessage("Nenhum peso na balança, realize a pesagem novamente")
        ScaleStatus.OVERWEIGHT.value -> emitMessage("Balança em sobrecarga, remove o peso extra")
        else -> updateWeightProduct(bytes)
      }
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

  private fun updateWeightProduct(message: ByteArray) {

    val newWeight: Double = Formatter.formatFromToledoProtocolP05(message)

    //weight = Formatter.formatFromUranoUseCb2Protocol(message)
    val currentProduct = _mgvProduct.value
    val updatedProduct = currentProduct?.copy(
      weight = newWeight,
      totalPrice = currentProduct.price.times(newWeight)
    )
    currentProduct?.let { loadPrice(it.price) }
    _mgvProduct.value = updatedProduct
    _updateStatus.update { StatusApp.GRAVAR_OPERACAO }
  }

  fun setActiveDevice(btConnected: BluetoothConnected) {
    _activeDevice.update { btConnected }
  }

  fun fetchItem(codItem: String) {
    Timber.i("fetchItem: Buscando item")
//    val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
    val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)
    if (mgvPassword == null) {
      showMessage("Senha do MGV não configurada")
      return
    }

    val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()
    if (codItem.isEmpty()) {
      showMessage(resourceProvider.getString(R.string.product_not_found))
      return
    }
    _updateStatus.update { StatusApp.BUSCAR_PRODUTO }
    viewModelScope.launch {
      try {
        val (mgvError, item) = withContext(Dispatchers.Default) {
          mgvService.queryItem(
            item = codItem,
            palavraChave = mgvPassword,
            numeroLoja = storeNumber
          )
        }

        if (mgvError != null) {
          Timber.i("fetchItem: ${mgvError.message}")
          showMessage(
            resourceProvider.getString(
              R.string.error_responde_mgv,
//              error.message,
//              error.code
            )
          )
          return@launch
        }


        item?.also { product ->
          product.extraInfo = item.codExtraInfo?.let { code ->
            getExtraInfo(mgvPassword, storeNumber, code)
          }
          product.nutrictionFacts = item.codNutrictionFacts?.let { code ->
            getNutriction(mgvPassword, storeNumber, code)
          }
          _mgvProduct.value = product

        }
        _updateStatus.update { StatusApp.REALIZAR_PESAGEM }
      } catch (e: IllegalArgumentException) {
        showMessage(resourceProvider.getString(R.string.invalid_address_server))
        Timber.e(e, "fetchItem: ")
      } catch (e: Exception) {
        showMessage(resourceProvider.getString(R.string.request_error))
        Timber.e(e, "fetchItem: ")
      }
    }
  }

  private suspend fun getNutriction(
    mgvPassword: String,
    storeNumber: String,
    codItem: Int
  ): MgvInformacoesNutricionais? {
    val responseNutritionalInfo = withContext(Dispatchers.Default) {
      mgvService.queryNutritionalInfo(
        palavraChave = mgvPassword,
        numeroLoja = storeNumber
      )
    }
    return responseNutritionalInfo.nutrictionFacts?.find {
      it.Codigo == codItem.toString()
    }
  }

  private suspend fun getExtraInfo(
    mgvPassword: String,
    storeNumber: String,
    codItem: Int
  ): ExtraInfo? {

    val responseExtraInfo = withContext(Dispatchers.Default) {
      mgvService.queryExtraInfo(palavraChave = mgvPassword, numeroLoja = storeNumber)
    }

    return responseExtraInfo.extraInfos?.find {
      it.code == codItem.toString()
    }
  }

  fun print() {
    if (checkProductIsNullOrEmpty()) {
      showMessage(resourceProvider.getString(R.string.error_to_get_product))
      return
    }

    val product = _mgvProduct.value
    if (product?.weight == null || product.weight == 0.0) {
      showMessage(resourceProvider.getString(R.string.make_weighing))
      return
    }

    viewModelScope.launch {
      try {
        val printerType = configRepository.getPrinterType()
        Timber.i("Impressora selecionada: %s", printerType)

        when (printerType) {
          PrinterType.USB -> {
            val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
            val savedDeviceName = configRepository.getUsbConfig()

            if (savedDeviceName == null) {
              showMessage("Nenhuma impressora USB configurada.")
              return@launch
            }

            val (printerModel, devicePath) = savedDeviceName.split(";")
            val usbDevice = usbManager.deviceList.values.find { it.deviceName == devicePath }

            if (usbDevice != null) {
              if (printerModel.contains("TM-L100", ignoreCase = true)) {
                epson.initializePrinter(savedDeviceName)

                val created = epson.createReceiptData(product)
                if (created) {
                  val success = epson.printData()
                  if (!success) {
                    showMessage("Erro ao imprimir na Epson")
                  }
                } else {
                  showMessage("Erro na definição Epson")
                }

              } else {
                honeywellPrinter.initializePrinter(usbDevice)

                val success = withContext(Dispatchers.IO) {
                  honeywellPrinter.safeConnectSuspend()
                }

                if (success) {
                  val zpl = label.generateLabelBase(product, null)
                  withContext(Dispatchers.IO) {
                    honeywellPrinter.printZpl(zpl)
                  }
                } else {
                  showMessage("Falha na conexão com a impressora Honeywell")
                }
              }

              clearFields()
              _updateStatus.update { StatusApp.FINALIZAR }

            } else {
              showMessage("Impressora USB '$devicePath' não encontrada.")
            }
          }

          PrinterType.NETWORK -> {
            val zpl = label.generateLabelBase(product, null)
            createNetworkTask(zpl)

            _scale.value = null
            _updateStatus.update { StatusApp.FINALIZAR }
            clearFields()
          }
        }


        //HONEYWELL
        /*
        when (printerType) {
          PrinterType.USB -> {
            val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
            val savedDeviceName = configRepository.getUsbConfig()

            if (savedDeviceName == null) {
              showMessage("Nenhuma impressora USB configurada.")
              return@launch
            }

            val printerName = savedDeviceName.split(";")
            val honeywellDevice = usbManager.deviceList.values.find {
              it.deviceName == printerName[1]
            }

            if (honeywellDevice != null) {
              honeywellPrinter.initializePrinter(honeywellDevice)

              val success = withContext(Dispatchers.IO) {
                honeywellPrinter.safeConnectSuspend()
              }

              if (success) {
                val zpl = label.generateLabelBase(product, null)

                withContext(Dispatchers.IO) {
                  honeywellPrinter.printZpl(zpl)
                }

                clearFields()
                _updateStatus.update { StatusApp.FINALIZAR }
              } else {
                showMessage("Falha na conexão com a impressora")
              }
            } else {
              showMessage("Impressora USB '$savedDeviceName' não encontrada")
            }
          }

          PrinterType.NETWORK -> {
            val zpl = label.generateLabelBase(product, null)
            createNetworkTask(zpl)

            _scale.value = null
            _updateStatus.update { StatusApp.FINALIZAR }
            clearFields()
          }
        }
        */

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
      val operation = _mgvProduct.value?.let { product ->
        Operation(
          weight = product.weight!!,
          totalPrice = product.totalPrice!!,
          pricePerKilogram = product.price,
          username = Build.ID,
          description = product.description
        )
      } ?: throw Exception(resourceProvider.getString(R.string.save_operation_error))

      viewModelScope.launch {
        operationRepository.save(operation)
      }

      _updateStatus.value = StatusApp.REALIZAR_IMPRESSAO

    } catch (e: Exception) {
      Timber.i("saveOperation: Erro ao salvar operação", e)
      showMessage(e.message!!)
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