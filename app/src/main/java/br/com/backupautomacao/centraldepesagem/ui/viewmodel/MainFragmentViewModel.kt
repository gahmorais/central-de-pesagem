package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.compose.ui.Modifier
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CONNECTED_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CONNECTING_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_GET_PRODUCT
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_MAKE_WEIGHING
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_MESSAGE
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_PRINTER_NOT_FOUND
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_PRODUCT_NOT_FOUND
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_REQUEST
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_RESPONSE_MGV
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_TO_SET_SCALE
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_CONNECTING
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_ERROR_IP_NOT_SETTED
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_PRINTING_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_READ
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_SCALE_CONNECTED
import br.com.backupautomacao.centraldepesagem.Printer
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectedThread
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectingThread
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.network.MgvService
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
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors


enum class ProcessStatus {
  INICIO,
  BUSCAR_PRODUTO,
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

        if (msg.what == MESSAGE_READ) {
          val message = msg.obj as ByteArray
          val updateWeightProduct = _mgvProduct.value
          updateWeightProduct?.weight = Formatter.formatFromUranoUseCb2Protocol(message)
          updateWeightProduct?.totalPrice = updateWeightProduct!!.Preco.times(updateWeightProduct.weight!!)
          _mgvProduct.value = updateWeightProduct
          _updateStatus.value = ProcessStatus.REALIZAR_IMPRESSAO
        }

        if (msg.what == CONNECTING_STATUS && msg.arg1 == 1) {
          _statusMessage.value = MESSAGE_CONNECTING
        }
        if (msg.what == CONNECTED_STATUS) {
          val thread = msg.obj as ConnectedThread
          _connectedThreads.value?.add(thread)
          if (_connectedThreads.value?.size == 1) {
            val threadIndex = 0
            setActiveThread(threadIndex)
          }
          val scaleNumber = connectedThreads.value?.size
          _statusMessage.value = MESSAGE_SCALE_CONNECTED(scaleNumber)
        }
        if (msg.what == ERROR_MESSAGE) {
          val errorMessage = msg.obj as String
          _statusMessage.value = errorMessage
        }
      }
    }
  }

  fun fetchItem(codItem: String) {
    Log.i("MainFragmentViewModel", "fetchItem: Buscando item")
    if (codItem.isEmpty()) {
      _statusMessage.value = "Digite o código do item"
      return
    }
    _updateStatus.value = ProcessStatus.BUSCAR_PRODUTO
    viewModelScope.launch {
      try {
        val (ConsultaItemResult, item) = withContext(Dispatchers.Default) {
          retrofitInstance.queryItem(item = codItem)
        }

        ConsultaItemResult?.let { error ->
          _statusMessage.value = ERROR_RESPONSE_MGV(error.Msg, error.Codigo)
        }
        item?.also { product ->
          val responseNutritionalInfo = withContext(Dispatchers.Default) {
            retrofitInstance.queryNutritionalInfo()
          }

          val responseExtraInfo = withContext(Dispatchers.Default) {
            retrofitInstance.queryExtraInfo()
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
        _statusMessage.value = "Endereço do servidor inválido"
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      } catch (e: Exception) {
        _statusMessage.value = ERROR_REQUEST
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      }
    }
  }

  fun makeWeighing() {
    if (checkProductIsNullOrEmpty()) {
      _statusMessage.value = ERROR_GET_PRODUCT("pesagem")
      return
    }
    val command = byteArrayOf(0x05)
    _currentDeviceConnected.value?.write(command)
  }

  fun makePrint() {

    val printerIp: String? = configRepository.getConfig(PRINTER_IP)
    if (printerIp == "" || printerIp == null) {
      _statusMessage.value = MESSAGE_ERROR_IP_NOT_SETTED
      return
    }

    if (checkProductIsNullOrEmpty()) {
      _statusMessage.value = ERROR_GET_PRODUCT("impressão")
      return
    }

    val product = _mgvProduct.value!!
    if (product.weight == null || product.weight == 0.0) {
      _statusMessage.value = ERROR_MAKE_WEIGHING
      return
    }

    try {
      val label = Label()
//      val newLabel = product.nutrictionFacts?.let {
//        label.createLabelRdc429(product)
//      } ?: label.createSimpleLabel(product)
      val newLabel = label.createLabelBase(product)
      Log.i("MainFragmentViewModel", "makePrint: $newLabel")
      val printer = Printer(printerIp)
      printer.print(newLabel)
    } catch (e: ExecutionException) {
      _statusMessage.value = ERROR_PRINTER_NOT_FOUND
      Log.e("MainFragmentViewModel", "makePrint: ${e.message}", e)
    } catch (e: Exception) {
      _statusMessage.value = MESSAGE_PRINTING_ERROR
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
    _statusMessage.value = "Ouvindo..."
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
      val scaleNumber = Util.stringToNumberTransform(index) - 1
      val code = findItem(product)
      Log.i("MainFragmentViewModel", "executeTask: scale: $scaleNumber")
      Log.i("MainFragmentViewModel", "executeTask: Número Balança -> $index")
      Log.i("MainFragmentViewModel", "executeTask: Produto -> $product")
      Log.i("MainFragmentViewModel", "executeTask: Código -> $code")
      if (scaleNumber < 0) {
        _statusMessage.value = ERROR_TO_SET_SCALE
        return
      }
      if (code == 0) {
        _statusMessage.value = ERROR_PRODUCT_NOT_FOUND
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