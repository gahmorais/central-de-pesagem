package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_CONNECTED_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_CONNECTING_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_ERROR_MESSAGE
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_MESSAGE_READ
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ITEM_CODE_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.IP_NOT_SET_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.LISTENING_MESSAGE
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MAKE_WEIGHING_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_CONNECTING
import br.com.backupautomacao.centraldepesagem.Constants.Companion.PORT_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.PRINTER_NOT_FOUND_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.PRINTING_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.PRODUCT_NOT_FOUND_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.REQUEST_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.SERVER_ADDRESS_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.SET_SCALE_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.errorGetProdut
import br.com.backupautomacao.centraldepesagem.Constants.Companion.errorResponseMgv
import br.com.backupautomacao.centraldepesagem.Constants.Companion.messageScaleConnected
import br.com.backupautomacao.centraldepesagem.Printer
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectedThread
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectingThread
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository.Companion.PRINTER_IP
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.Util
import br.com.backupautomacao.centraldepesagem.utils.findItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.gotev.speech.Speech
import net.gotev.speech.SpeechDelegate
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors


enum class ProcessStatus {
  INICIO,
  BUSCAR_PRODUTO,
  CARREGAR_PRECO,
  REALIZAR_PESAGEM,
  REALIZAR_IMPRESSAO,
  FINALIZAR
}

class MainFragmentViewModel(
  private val configRepository: ConfigRepository,
  private val bluetoothAdapter: BluetoothAdapter
) : ViewModel(), SpeechDelegate {

  private val retrofitInstance by lazy {
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    MgvApi.retrofitService(serverAddress)
  }

  private val speechInstance: Speech = Speech.getInstance()
  private val _mgvProduct: MutableLiveData<MgvProduct?> = MutableLiveData()
  val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

  private val _statusMessage: MutableLiveData<String> = MutableLiveData()
  val statusMessage: LiveData<String> = _statusMessage

  private val _scale: MutableStateFlow<Int?> = MutableStateFlow(null)
  val scale: StateFlow<Int?> = _scale

  private val _connectedThreads: MutableLiveData<ArrayList<ConnectedThread>> =
    MutableLiveData(ArrayList())
  val connectedThreads: LiveData<ArrayList<ConnectedThread>> = _connectedThreads

  private val _updateStatus: MutableStateFlow<ProcessStatus> =
    MutableStateFlow(ProcessStatus.INICIO)

  init {
    viewModelScope.launch {
      _updateStatus.collectLatest { status ->
        val automaticPrint = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
        when (status) {
          ProcessStatus.INICIO -> Log.i("MainFragmentViewModel", "Inicio: ")
          ProcessStatus.BUSCAR_PRODUTO -> Log.i("MainFragmentViewModel", "Buscando produto")
          ProcessStatus.REALIZAR_PESAGEM -> makeWeighing()
          ProcessStatus.REALIZAR_IMPRESSAO -> if (automaticPrint) makePrint()
          else -> Log.i("MainFragmentViewModel", "Processo finalizado")
        }
      }
    }
  }

  private val _currentDeviceConnected: MutableLiveData<ConnectedThread> = MutableLiveData()

  private val mHandler: Handler by lazy {
    object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        if (msg.what == CODE_MESSAGE_READ) {
          val message = msg.obj as ByteArray
          val newWeight: Double = Formatter.formatFromToledoProtocolP05(message)
          val updateProduct = _mgvProduct.value!!
          updateProduct.apply {
          //weight = Formatter.formatFromUranoUseCb2Protocol(message)
            weight = newWeight
            totalPrice = Preco.times(updateProduct.weight!!)
            loadPrice(Preco)
          }
          _mgvProduct.value = updateProduct
          _updateStatus.value = ProcessStatus.REALIZAR_IMPRESSAO
        }

        if (msg.what == CODE_CONNECTING_STATUS && msg.arg1 == 1) {
          _statusMessage.value = MESSAGE_CONNECTING
        }
        if (msg.what == CODE_CONNECTED_STATUS) {
          val thread = msg.obj as ConnectedThread
          _connectedThreads.value?.add(thread)
          if (_connectedThreads.value?.size == 1) {
            val firstDevice = 0
            setActiveThread(firstDevice)
          }
          val scaleNumber = connectedThreads.value?.size
          _statusMessage.value = messageScaleConnected(scaleNumber)
        }
        if (msg.what == CODE_ERROR_MESSAGE) {
          val errorMessage = msg.obj as String
          _statusMessage.value = errorMessage
        }
      }
    }
  }

  fun fetchItem(codItem: String) {
    Log.i("MainFragmentViewModel", "fetchItem: Buscando item")
    val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
    val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()
    if (codItem.isEmpty()) {
      _statusMessage.value = ITEM_CODE_ERROR
      return
    }
    _updateStatus.value = ProcessStatus.BUSCAR_PRODUTO
    viewModelScope.launch {
      try {
        val (ConsultaItemResult, item) = withContext(Dispatchers.Default) {
          retrofitInstance.queryItem(item = codItem, palavraChave = mgvPassword, numeroLoja = storeNumber)
        }

        ConsultaItemResult?.let { error ->
          _statusMessage.value = errorResponseMgv(error.Msg, error.Codigo)
        }
        item?.also { product ->
          val responseNutritionalInfo = withContext(Dispatchers.Default) {
            retrofitInstance.queryNutritionalInfo(palavraChave = mgvPassword, numeroLoja = storeNumber)
          }

          val responseExtraInfo = withContext(Dispatchers.Default) {
            retrofitInstance.queryExtraInfo(palavraChave = mgvPassword, numeroLoja = storeNumber)
          }
          val (informacoesNutricionais) = responseNutritionalInfo
          val (informacoesExtra) = responseExtraInfo
          val nutrictionFact = informacoesNutricionais?.find {
            it.Codigo == product.CodInfoNutricional.toString()
          }

          val extraInfo = informacoesExtra?.find {
            it.Codigo == product.CodInfoExtra.toString()
          }
          product.extraInfo = extraInfo
          product.nutrictionFacts = nutrictionFact
          _mgvProduct.value = product

          Log.i("MainFragmentViewModel", "fetchItem: Realizando pesagem")
          _updateStatus.value = ProcessStatus.REALIZAR_PESAGEM

        }
      } catch (e: IllegalArgumentException) {
        _statusMessage.value = SERVER_ADDRESS_ERROR
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      } catch (e: Exception) {
        _statusMessage.value = REQUEST_ERROR
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      }
    }
  }

  fun makeWeighing() {
    if (checkProductIsNullOrEmpty()) {
      _statusMessage.value = errorGetProdut("pesagem")
      return
    }
    val command = byteArrayOf(0x05)
    _currentDeviceConnected.value?.write(command)
  }

  fun makePrint() {

    val printerIp: String? = configRepository.getConfig(PRINTER_IP)
    if (printerIp == "" || printerIp == null) {
      _statusMessage.value = IP_NOT_SET_ERROR
      return
    }

    val printerPort: Int = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    if(printerPort == 0){
      _statusMessage.value = PORT_ERROR
      return
    }

    if (checkProductIsNullOrEmpty()) {
      _statusMessage.value = errorGetProdut("impressão")
      return
    }

    val product = _mgvProduct.value!!
    if (product.weight == null || product.weight == 0.0) {
      _statusMessage.value = MAKE_WEIGHING_ERROR
      return
    }

    try {
      val label = Label()
      val newLabel = label.createLabelBase(product)
      Log.i("MainFragmentViewModel", "makePrint: $newLabel")
      val printer = Printer(printerIp, printerPort)
      printer.print(newLabel)
    } catch (e: ExecutionException) {
      _statusMessage.value = PRINTER_NOT_FOUND_ERROR
      Log.e("MainFragmentViewModel", "makePrint: ${e.message}", e)
    } catch (e: Exception) {
      _statusMessage.value = PRINTING_ERROR
      Log.e("MainFragmentViewModel", "makePrint: ${e.message}", e)
    } finally {
      clearFields()
      _scale.value = null
      _updateStatus.value = ProcessStatus.FINALIZAR
    }
  }

  private fun checkProductIsNullOrEmpty(): Boolean {
    return _mgvProduct.value?.run {
      Codigo.isEmpty()
    } ?: true
  }

  @Throws(SecurityException::class)
  fun getAvailableDevices(): LiveData<List<BluetoothDevice>> {

    val devices = bluetoothAdapter.bondedDevices
      .filter { it.name.contains("BT_URA") }
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

  fun loadPrice(value: Double) {
    val priceByteArray = Formatter.formatDataToLoadPriceToledo(value)
    _currentDeviceConnected.value?.write(priceByteArray)
  }

  private fun clearFields() {
    _mgvProduct.value = MgvProduct()
  }

  fun connectToDevices(devices: List<BluetoothDevice>) {
    devices.forEach { device ->
      val executor = Executors.newSingleThreadExecutor()
      val newThread = ConnectingThread(bluetoothAdapter, device.address, mHandler)
      executor.execute(newThread)
      executor.shutdown()
    }
  }

  override fun onStartOfSpeech() {
    _statusMessage.value = LISTENING_MESSAGE
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
    speechInstance.startListening(this)
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
      Log.i("MainFragmentViewModel", "executeTask: scale: $scaleNumber")
      Log.i("MainFragmentViewModel", "executeTask: Número Balança -> $index")
      Log.i("MainFragmentViewModel", "executeTask: Produto -> $product")
      Log.i("MainFragmentViewModel", "executeTask: Código -> $code")
      if (scaleNumber < 0) {
        _statusMessage.value = SET_SCALE_ERROR
        return
      }
      if (code == 0) {
        _statusMessage.value = PRODUCT_NOT_FOUND_ERROR
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
}