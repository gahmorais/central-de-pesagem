package br.com.backupautomacao.centraldepesagem.ui.viewmodel


import android.app.Application
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.PrintWorker
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.bluetooth.BluetoothListener
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectedThread
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectingThread
import br.com.backupautomacao.centraldepesagem.db.model.Operation
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository.Companion.PRINTER_IP
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import br.com.backupautomacao.centraldepesagem.network.ExtraInfo
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvInformacoesNutricionais
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.services.PrinterUsb
import br.com.backupautomacao.centraldepesagem.ui.UiHandler
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider
import br.com.backupautomacao.centraldepesagem.utils.Util
import br.com.backupautomacao.centraldepesagem.utils.findItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.gotev.speech.Speech
import net.gotev.speech.SpeechDelegate
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.security.InvalidParameterException
import java.util.concurrent.Executors


enum class StatusApp {
  INICIO,
  BUSCAR_PRODUTO,
  CARREGAR_PRECO,
  REALIZAR_PESAGEM,
  REALIZAR_IMPRESSAO,
  GRAVAR_OPERACAO,
  FINALIZAR
}

class MainFragmentViewModel(
  private val printerUsb: PrinterUsb,
  private val configRepository: ConfigRepository,
  private val operationRepository: OperationRepository,
  private val workManager: WorkManager,
  private val bluetoothAdapter: BluetoothAdapter,
  private val resourceProvider: ResourceProvider,
  private val label: Label

) : ViewModel(),
  SpeechDelegate, BluetoothListener {

  private val retrofitInstance by lazy {
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    MgvApi.retrofitService(serverAddress)
  }

  private val _mgvProduct: MutableLiveData<MgvProduct?> = MutableLiveData()
  val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

  private val _statusMessage: MutableLiveData<String> = MutableLiveData()
  val statusMessage: LiveData<String> = _statusMessage

  private val _scale: MutableStateFlow<Int?> = MutableStateFlow(null)
  val scale: StateFlow<Int?> = _scale

  private val _usbDevice: MutableStateFlow<UsbDevice?> = MutableStateFlow(null)
  val usbDevice = _usbDevice.asStateFlow()

  private val _connectedThreads: MutableLiveData<ArrayList<ConnectedThread>> =
    MutableLiveData(ArrayList())
  val connectedThreads: LiveData<ArrayList<ConnectedThread>> = _connectedThreads

  private val _updateStatus: MutableStateFlow<StatusApp> = MutableStateFlow(StatusApp.INICIO)

  init {
//    val filter = IntentFilter(ACTION_USB_PERMISSION)
//    context.registerReceiver(usbReceiver, filter)
    applicationMonitor()
  }

  fun openConnectionUSB(context: Context){
    printerUsb.openConnection(context)
  }

  fun connecToDevice(){
    val device = printerUsb.connectToDevice()
    _usbDevice.update { device }
  }

  fun requestPermissionUSB(context: Context){
    _usbDevice.value?.let { printerUsb.requestPermission(it, context) }
  }

  private fun applicationMonitor() {
    viewModelScope.launch {
      _updateStatus.collectLatest { status ->
        val automaticPrint = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
        when (status) {
          StatusApp.INICIO -> Log.i("MainFragmentViewModel", "Inicio: ")
          StatusApp.BUSCAR_PRODUTO -> Log.i("MainFragmentViewModel", "Buscando produto")
          StatusApp.REALIZAR_PESAGEM -> weighing()
          StatusApp.GRAVAR_OPERACAO -> saveOperation()
          StatusApp.REALIZAR_IMPRESSAO -> if (automaticPrint) print()
          else -> Log.i("MainFragmentViewModel", "Processo finalizado")
        }
      }
    }
  }

  private val _currentDeviceConnected: MutableLiveData<ConnectedThread> = MutableLiveData()

  private fun updateWeightProduct(message: ByteArray) {

    val newWeight: Double = Formatter.formatFromToledoProtocolP05(message)

    //weight = Formatter.formatFromUranoUseCb2Protocol(message)
    val updateProduct = _mgvProduct.value!!
    updateProduct.apply {
      weight = newWeight
      totalPrice = price.times(updateProduct.weight!!)
      loadPrice(price)
    }
    _mgvProduct.value = updateProduct
    _updateStatus.value = StatusApp.GRAVAR_OPERACAO
  }

  fun fetchItem(codItem: String) {
    Log.i("MainFragmentViewModel", "fetchItem: Buscando item")
    val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
    val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()
    if (codItem.isEmpty()) {
      showMessage(resourceProvider.getString(R.string.barcode_error))
      return
    }
    _updateStatus.value = StatusApp.BUSCAR_PRODUTO
    viewModelScope.launch {
      try {
        val (mgvError, item) = withContext(Dispatchers.Default) {
          retrofitInstance.queryItem(
            item = codItem,
            palavraChave = mgvPassword,
            numeroLoja = storeNumber
          )
        }

        mgvError?.let { error ->
          Log.i("MainFragmentViewModel", "fetchItem: $error")
          showMessage(
            resourceProvider.getString(
              R.string.error_responde_mgv,
//              error.message,
//              error.code
            )
          )
        }

        item?.also { product ->
          product.extraInfo = item.codExtraInfo?.let { code ->
            getExtraInfo(mgvPassword, storeNumber, code)
          }
          product.nutrictionFacts = item.codNutrictionFacts?.let { code ->
            getNutriction(mgvPassword, storeNumber, code)
          }
          _mgvProduct.value = product
          _updateStatus.value = StatusApp.REALIZAR_PESAGEM
        }

      } catch (e: IllegalArgumentException) {
        showMessage(resourceProvider.getString(R.string.invalid_address_server))
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      } catch (e: Exception) {
        showMessage(resourceProvider.getString(R.string.request_error))
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      }
    }
  }

  private suspend fun getNutriction(
    mgvPassword: String,
    storeNumber: String,
    codItem: Int
  ): MgvInformacoesNutricionais? {
    val responseNutritionalInfo = withContext(Dispatchers.Default) {
      retrofitInstance.queryNutritionalInfo(
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
      retrofitInstance.queryExtraInfo(palavraChave = mgvPassword, numeroLoja = storeNumber)
    }

    return responseExtraInfo.extraInfos?.find {
      it.code == codItem.toString()
    }
  }

  fun weighing() {
    Log.i("MainFragmentViewModel", "weighing: Realizando pesagem")
    if (checkProductIsNullOrEmpty()) {
      showMessage(resourceProvider.getString(R.string.error_get_product))
      return
    }
    val command = byteArrayOf(0x05)
    _currentDeviceConnected.value?.write(command)
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

    try {
      val printerIp = configRepository.getConfig(PRINTER_IP)
      val printerPort = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
      val usbPrinterName = configRepository.getUsbPrinterName()
      val printerType = configRepository.getPrinterType()
      val newLabel = label.generateLabelBase(product, null)

      Log.i("PrinterType", "Type: $printerType")
      Log.i("PrinterUSB", "USB Name: $usbPrinterName")

      val printTask = when {
        printerType.equals("USB", ignoreCase = true) -> PrintWorker.createUniquePrintTask(
          printerAddress = null,
          printerPort = 0,
          label = newLabel,
          usbPrinterName = usbPrinterName
        )
        else -> PrintWorker.createUniquePrintTask(
          printerAddress = printerIp,
          printerPort = printerPort,
          label = newLabel,
          usbPrinterName = null
        )
      }

      workManager.enqueueUniqueWork(PrintWorker.PRINT_TASK, ExistingWorkPolicy.REPLACE, printTask)
      _scale.value = null
      _updateStatus.value = StatusApp.FINALIZAR
      clearFields()
    } catch (e: FileNotFoundException) {
      showMessage(resourceProvider.getString(R.string.file_not_found_error))
    } catch (e: InvalidParameterException) {
      showMessage(resourceProvider.getString(R.string.invalid_printer_config))
    } catch (e: Exception) {
      showMessage("${resourceProvider.getString(R.string.unexpected_error)}: ${e.localizedMessage}")
    }
  }
  
  private fun checkProductIsNullOrEmpty(): Boolean {
    return _mgvProduct.value?.run {
      code.isEmpty()
    } ?: true
  }

  @Throws(SecurityException::class)
  fun getAvailableDevices(): LiveData<List<BluetoothDevice>> {
    val devices = bluetoothAdapter.bondedDevices
      .filter { it.name.contains("BT_URA") || it.name.contains("BKP") }
      .sortedByDescending { it.address }

    return MutableLiveData(devices)
  }

  fun setActiveThread(currentThread: Int) {
    _connectedThreads.value.let { devicesConnected ->
      devicesConnected?.let {
        _currentDeviceConnected.value = it[currentThread]
      }
    }
  }

  fun showMessage(message: String = "") {
    _statusMessage.value = message
  }

  private fun loadPrice(value: Double) {
    val priceByteArray = Formatter.formatDataToLoadPriceToledo(value)
    _currentDeviceConnected.value?.write(priceByteArray)
  }

  private fun clearFields() {
    _mgvProduct.value = MgvProduct()
  }

  fun connectToDevices(devices: List<BluetoothDevice>) {
    val uiHandler = UiHandler(this)
    devices.forEach { device ->
      val executor = Executors.newSingleThreadExecutor()
      val newThread = ConnectingThread(bluetoothAdapter, device.address, uiHandler)
      executor.execute(newThread)
      executor.shutdown()
    }
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
      Log.e("MainFragmentViewModel", "saveOperation: Erro ao salvar operação", e)
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
      Log.i("MainFragmentViewModel", "onSpeechResult: $it")
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
      Log.i("MainFragmentViewModel", "executeTask: Balança: $scaleNumber")
      Log.i("MainFragmentViewModel", "executeTask: Número Balança -> $index")
      Log.i("MainFragmentViewModel", "executeTask: Produto -> $product")
      Log.i("MainFragmentViewModel", "executeTask: Código -> $code")
      if (scaleNumber < 0) {
        showMessage(resourceProvider.getString(R.string.error_to_set_scale))
        return
      }
      if (code == 0) {
        showMessage(resourceProvider.getString(R.string.product_not_found))
        return
      }
      _scale.value = scaleNumber
      Log.i("MainFragmentViewModel", "executeTask: Indice Balança ${_scale.value}")
      _connectedThreads.value?.let { setActiveThread(scaleNumber) }

      fetchItem(codItem = code.toString())
    }
  }


  override fun onCleared() {
    super.onCleared()
    Speech.getInstance().shutdown()
    _connectedThreads.value?.run {
      forEach { it.cancel() }
    }
  }

  override fun onBluetoothRead(data: ByteArray) {
    val msgString = String(data, StandardCharsets.UTF_8)
    Log.i("MainFragmentViewModel", "onBluetoothRead: Recebendo peso")
    if (Formatter.removeNonAlphaCharacters(msgString) == "IIIII") {
      showMessage(resourceProvider.getString(R.string.unstable_weight))
    } else {
      updateWeightProduct(data)
    }
  }

  override fun onBluetoothConnecting() {
    showMessage(resourceProvider.getString(R.string.connecting))
  }

  override fun onBluetoothConnected(device: ConnectedThread) {
    _connectedThreads.value?.add(device)
    if (_connectedThreads.value?.size == 1) {
      val firstDevice = 0
      setActiveThread(firstDevice)
    }
    val  scaleNumber = connectedThreads.value?.size

    showMessage(
      resourceProvider.getString(
        R.string.message_scale_connected,
//        scaleNumber
      )
    )
  }

  override fun onBluetoothError(message: String) {
    showMessage(message)
  }
}