package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_CONNECTED_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_CONNECTING_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_ERROR_MESSAGE
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_MESSAGE_READ
import br.com.backupautomacao.centraldepesagem.PrintWorker
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectedThread
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectingThread
import br.com.backupautomacao.centraldepesagem.db.model.Operation
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository.Companion.PRINTER_IP
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
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
import java.nio.charset.StandardCharsets
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

class MainFragmentViewModel(application: Application) :
  AndroidViewModel(application),
  SpeechDelegate {

  private val speechInstance by lazy { Speech.getInstance() }
  private val configRepository by lazy { getApplication<AppApplication>().configRepository }
  private val operationRepository by lazy { getApplication<AppApplication>().operationRepository }
  private val workManager by lazy { getApplication<AppApplication>().workManager }

  private val bluetoothAdapter by lazy {
    val bm = getApplication<AppApplication>().getSystemService(BluetoothManager::class.java)
    bm.adapter
  }

  private val retrofitInstance by lazy {
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    MgvApi.retrofitService(serverAddress)
  }

  private val _mgvProduct: MutableLiveData<MgvProduct?> = MutableLiveData()
  val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

  private val _statusMessage: MutableLiveData<String> = MutableLiveData()
  val statusMessage: LiveData<String> = _statusMessage

  fun printStatus() = workManager.getWorkInfosForUniqueWorkLiveData(PrintWorker.PRINT_TASK)

  private val _scale: MutableStateFlow<Int?> = MutableStateFlow(null)
  val scale: StateFlow<Int?> = _scale

  private val _connectedThreads: MutableLiveData<ArrayList<ConnectedThread>> =
    MutableLiveData(ArrayList())
  val connectedThreads: LiveData<ArrayList<ConnectedThread>> = _connectedThreads

  private val _updateStatus: MutableStateFlow<StatusApp> = MutableStateFlow(StatusApp.INICIO)

  init {
    viewModelScope.launch {
      _updateStatus.collectLatest { status ->
        val automaticPrint = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
        when (status) {
          StatusApp.INICIO -> Log.i("MainFragmentViewModel", "Inicio: ")
          StatusApp.BUSCAR_PRODUTO -> Log.i("MainFragmentViewModel", "Buscando produto")
          StatusApp.REALIZAR_PESAGEM -> makeWeighing()
          StatusApp.GRAVAR_OPERACAO -> saveOperation()
          StatusApp.REALIZAR_IMPRESSAO -> if (automaticPrint) makePrint()
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
          val msgString = String(message, StandardCharsets.UTF_8)
          if (Formatter.removeNonAlphaCharacters(msgString) == "IIIII") {
            showMessage(getString(R.string.unstable_weight))
          } else {
            updateWeightProduct(message)
          }
        }

        if (msg.what == CODE_CONNECTING_STATUS && msg.arg1 == 1) {
          showMessage(getString(R.string.connecting))
        }
        if (msg.what == CODE_CONNECTED_STATUS) {
          val thread = msg.obj as ConnectedThread
          _connectedThreads.value?.add(thread)
          if (_connectedThreads.value?.size == 1) {
            val firstDevice = 0
            setActiveThread(firstDevice)
          }
          val scaleNumber = connectedThreads.value?.size
          showMessage(getApplication<AppApplication>().getString(R.string.message_scale_connected, scaleNumber))
        }
        if (msg.what == CODE_ERROR_MESSAGE) {
          val errorMessage = msg.obj as String
          showMessage(errorMessage)
        }
      }
    }
  }

  private fun updateWeightProduct(message: ByteArray) {

    val newWeight: Double = Formatter.formatFromToledoProtocolP05(message)
    //weight = Formatter.formatFromUranoUseCb2Protocol(message)
    val updateProduct = _mgvProduct.value!!
    updateProduct.apply {
      weight = newWeight
      totalPrice = Preco.times(updateProduct.weight!!)
      loadPrice(Preco)
    }
    _mgvProduct.value = updateProduct
    _updateStatus.value = StatusApp.GRAVAR_OPERACAO
  }

  fun fetchItem(codItem: String) {
    Log.i("MainFragmentViewModel", "fetchItem: Buscando item")
    val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
    val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()
    if (codItem.isEmpty()) {
      showMessage(getApplication<AppApplication>().getString(R.string.barcode_error))
      return
    }
    _updateStatus.value = StatusApp.BUSCAR_PRODUTO
    viewModelScope.launch {
      try {
        val (ConsultaItemResult, item) = withContext(Dispatchers.Default) {
          retrofitInstance.queryItem(
            item = codItem,
            palavraChave = mgvPassword,
            numeroLoja = storeNumber
          )
        }

        ConsultaItemResult?.let { error ->
          showMessage(getString(R.string.error_responde_mgv, error.Msg, error.Codigo))
        }
        item?.also { product ->
          val responseNutritionalInfo = withContext(Dispatchers.Default) {
            retrofitInstance.queryNutritionalInfo(
              palavraChave = mgvPassword,
              numeroLoja = storeNumber
            )
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
          _updateStatus.value = StatusApp.REALIZAR_PESAGEM

        }
      } catch (e: IllegalArgumentException) {
        showMessage(getString(R.string.invalid_address_server))
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      } catch (e: Exception) {
        showMessage(getString(R.string.request_error))
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      }
    }
  }

  fun makeWeighing() {
    if (checkProductIsNullOrEmpty()) {
      showMessage(getString(R.string.error_get_product))
      return
    }
    val command = byteArrayOf(0x05)
    _currentDeviceConnected.value?.write(command)
  }

  fun makePrint() {

    val printerIp: String? = configRepository.getConfig(PRINTER_IP)
    if (printerIp == "" || printerIp == null) {
      showMessage(getString(R.string.ip_not_setted))
      return
    }

    val printerPort: Int = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    if (printerPort == 0) {
      showMessage(getString(R.string.invalid_port))
      return
    }

    if (checkProductIsNullOrEmpty()) {
      showMessage(getString(R.string.error_to_get_product))
      return
    }

    val product = _mgvProduct.value!!
    if (product.weight == null || product.weight == 0.0) {
      showMessage(getString(R.string.make_weighing))
      return
    }

    val label = Label()
    val newLabel = label.createLabelBase(product)
    val printTask = PrintWorker.createUniquePrintTask(
      printerAddress = printerIp,
      printerPort = printerPort,
      label = newLabel
    )

    workManager.enqueueUniqueWork(PrintWorker.PRINT_TASK, ExistingWorkPolicy.REPLACE, printTask)
    _scale.value = null
    _updateStatus.value = StatusApp.FINALIZAR
    clearFields()

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

  fun showMessage(message: String) {
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
    devices.forEach { device ->
      val executor = Executors.newSingleThreadExecutor()
      val newThread = ConnectingThread(bluetoothAdapter, device.address, mHandler)
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
          pricePerKilogram = product.Preco,
          username = "Gabriel",
          description = product.Descritivo1aLinha
        )
      } ?: throw Exception("Erro ao salvar operação")

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
    showMessage(getString(R.string.listening))
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
      Log.i("MainFragmentViewModel", "executeTask: scale: $scaleNumber")
      Log.i("MainFragmentViewModel", "executeTask: Número Balança -> $index")
      Log.i("MainFragmentViewModel", "executeTask: Produto -> $product")
      Log.i("MainFragmentViewModel", "executeTask: Código -> $code")
      if (scaleNumber < 0) {
        showMessage(getString(R.string.error_to_set_scale))
        return
      }
      if (code == 0) {
        showMessage(getString(R.string.product_not_found))
        return
      }
      _scale.value = scaleNumber
      Log.i("MainFragmentViewModel", "executeTask: Indice Balança ${_scale.value}")
      _connectedThreads.value?.let { setActiveThread(scaleNumber) }

      fetchItem(codItem = code.toString())
    }
  }

  private fun getString(stringRes: Int, vararg inputs: Any): String {

    return getApplication<AppApplication>().getString(stringRes, inputs)
  }

  override fun onCleared() {
    super.onCleared()
    Speech.getInstance().shutdown()
    _connectedThreads.value?.run {
      forEach { it.cancel() }
    }
  }
}

