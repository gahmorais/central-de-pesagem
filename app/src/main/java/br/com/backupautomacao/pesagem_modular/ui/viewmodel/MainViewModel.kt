package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import br.com.backupautomacao.pesagem_modular.PrintWorker
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.bluetooth.AndroidBluetoothController
import br.com.backupautomacao.pesagem_modular.bluetooth.BluetoothListener
import br.com.backupautomacao.pesagem_modular.bluetooth.ConnectedThread
import br.com.backupautomacao.pesagem_modular.db.model.Operation
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository.Companion.PRINTER_IP
import br.com.backupautomacao.pesagem_modular.db.repository.OperationRepository
import br.com.backupautomacao.pesagem_modular.labels.LabelBuilder
import br.com.backupautomacao.pesagem_modular.models.MgvProductImpl
import br.com.backupautomacao.pesagem_modular.network.*
import br.com.backupautomacao.pesagem_modular.utils.Formatter
import br.com.backupautomacao.pesagem_modular.utils.StringResourceProvider
import br.com.backupautomacao.pesagem_modular.utils.properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.nio.charset.StandardCharsets
import java.security.InvalidParameterException


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
  private val configRepository: ConfigRepository,
  private val operationRepository: OperationRepository,
  private val workManager: WorkManager,
  private val bluetoothController: AndroidBluetoothController,
  private val labelBuild: LabelBuilder,
  private val stringResourceProvider: StringResourceProvider,
  private val mgvRepository: MgvRepository
) :
  ViewModel(), BluetoothListener {

  private val _labelsList = MutableLiveData<List<String>>(listOf())
  val labelsList: LiveData<List<String>> = _labelsList

  private val _labelType = MutableLiveData<String>("")
  val labelType: LiveData<String> = _labelType
  val isFixed = MutableLiveData(false)

  private val _mgvProductImpl: MutableLiveData<MgvProductImpl?> = MutableLiveData()
  val mgvProductImpl: LiveData<MgvProductImpl?> = _mgvProductImpl

  private val _statusMessage: MutableLiveData<String> = MutableLiveData()
  val statusMessage: LiveData<String> = _statusMessage

  private val _connectedThreads: MutableLiveData<ArrayList<ConnectedThread>> =
    MutableLiveData(ArrayList())
  private val connectedThreads: LiveData<ArrayList<ConnectedThread>> = _connectedThreads
  private val _currentDeviceConnected: MutableLiveData<ConnectedThread> = MutableLiveData()
  private val _updateStatus: MutableStateFlow<StatusApp> = MutableStateFlow(StatusApp.INICIO)

  init {
    applicationMonitor()
    updateLabelList()
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

  private fun updateLabelList() {

    _labelsList.postValue(labelBuild.listLabels())
    _labelsList.value?.let {
      if (it.isNotEmpty()) {
        _labelType.postValue(labelsList.value?.get(0) ?: "")
      }
      _labelType.postValue("ET119")
    }
  }

  fun updateLabelType(label: String) {
    _labelType.postValue(label)
  }

  private fun updateWeightProduct(message: ByteArray) {
    val newWeight: Double = Formatter.formatFromToledoProtocolP05(message)
    val updateProduct = _mgvProductImpl.value!!

    updateProduct.apply {
      weight = newWeight
      totalPrice = price.times(updateProduct.weight!!)
      loadPrice(price)
    }

    _mgvProductImpl.value = updateProduct
    //Só grava a operação se o peso for maior que zero
    if (newWeight > 0.0) {
      _updateStatus.value = StatusApp.GRAVAR_OPERACAO
    }

  }

  fun fetchItem(codItem: String) {
    Log.i("MainFragmentViewModel", "fetchItem: Buscando item")

    if (codItem.isEmpty()) {
      showMessage(stringResourceProvider.getString(R.string.barcode_error))
      return
    }
    _updateStatus.value = StatusApp.BUSCAR_PRODUTO
    viewModelScope.launch(Dispatchers.IO) {
      try {

        val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
        val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()
        val product = mgvRepository.queryItem(mgvPassword, storeNumber, codItem)

        product?.codNutrictionFacts?.let {
          product.nutrictionFacts = mgvRepository.getNutriction(mgvPassword, storeNumber, it)
        }

        product?.codExtraInfo?.let {
          product.extraInfo = mgvRepository.getExtraInfo(mgvPassword, storeNumber, it)
        }

        _mgvProductImpl.postValue(product)
        _updateStatus.value = StatusApp.REALIZAR_PESAGEM
      } catch (e: NoRouteToHostException) {
        _statusMessage.postValue(stringResourceProvider.getString(R.string.invalid_address_server))
        Log.e("MainFragmentViewModel", "fetchItem: endereço inválido", e)
      } catch (e: ConnectException) {
        _statusMessage.postValue(stringResourceProvider.getString(R.string.fail_to_connect_server))
      } catch (e: Exception) {
        e.message?.let {
          _statusMessage.postValue(it)
          Log.e("MainFragmentViewModel", "fetchItem: ", e)
        }
      }
    }
  }

  fun weighing() {
    Log.i("MainFragmentViewModel", "weighing: Realizando pesagem")
    if (checkProductIsNullOrEmpty()) {
      showMessage(stringResourceProvider.getString(R.string.error_get_product))
      return
    }
    val command = byteArrayOf(0x05)
    _currentDeviceConnected.value?.write(command)
//    updateWeightProduct("5000".toByteArray())
  }

  fun print() {

    if (checkProductIsNullOrEmpty()) {
      showMessage(stringResourceProvider.getString(R.string.error_to_get_product))
      return
    }
    try {
      val printerIp: String? = configRepository.getConfig(PRINTER_IP)
      val printerPort: Int = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
      val product: MgvProductImpl? = _mgvProductImpl.value

      if ((product?.weight == null) || (product.weight == 0.0)) {
        showMessage(stringResourceProvider.getString(R.string.make_weighing))
        return
      }

      val label = labelBuild.build(_labelType.value ?: "")
      val newLabel = label.generateLabel(product).properties(1)

      val printTask = PrintWorker.createUniquePrintTask(
        printerAddress = printerIp,
        printerPort = printerPort,
        label = newLabel
      )

      workManager.enqueueUniqueWork(PrintWorker.PRINT_TASK, ExistingWorkPolicy.REPLACE, printTask)
      _updateStatus.value = StatusApp.FINALIZAR
      if (isFixed.value == false) {
        clearFields()
      } else {
        updateWeightProduct("0.0".toByteArray())
      }

    } catch (e: FileNotFoundException) {
      showMessage(stringResourceProvider.getString(R.string.file_not_found_error))
    } catch (e: InvalidParameterException) {
      showMessage("Porta ou endereço ip inválidos")
    } catch (e: Exception) {
      e.printStackTrace()
      showMessage("Erro não esperado")
    }
  }

  private fun checkProductIsNullOrEmpty(): Boolean {
    return _mgvProductImpl.value?.run {
      code.isEmpty()
    } ?: true
  }

  @Throws(SecurityException::class)
  fun getAvailableDevices(): LiveData<List<BluetoothDevice>> {
    val devices = bluetoothController.getPairedDevices()
    return MutableLiveData(devices)
  }

  private fun setActiveThread(currentThread: Int) {
    _connectedThreads.value.let { devicesConnected ->
      devicesConnected?.let {
        _currentDeviceConnected.value = it[currentThread]
      }
    }
  }

  fun showMessage(message: String = "") {
    _statusMessage.postValue(message)
  }

  private fun loadPrice(value: Double) {
    val priceByteArray = Formatter.formatDataToLoadPriceToledo(value)
    _currentDeviceConnected.value?.write(priceByteArray)
  }

  private fun clearFields() {
    _mgvProductImpl.value = null
  }

  fun connectToDevices(devices: List<BluetoothDevice>) = bluetoothController.connectToDevices(
    devices,
    this
  )

  private fun saveOperation() {
    try {
      val operation = _mgvProductImpl.value?.let { product ->
        Operation(
          weight = product.weight!!,
          totalPrice = product.totalPrice!!,
          pricePerKilogram = product.price,
          username = Build.ID,
          description = product.description
        )
      } ?: throw Exception(stringResourceProvider.getString(R.string.save_operation_error))

      viewModelScope.launch(Dispatchers.IO) {
        operationRepository.save(operation)
      }

      _updateStatus.value = StatusApp.REALIZAR_IMPRESSAO

    } catch (e: Exception) {
      Log.e("MainFragmentViewModel", "saveOperation: Erro ao salvar operação", e)
      showMessage(e.message!!)
    }
  }

  override fun onCleared() {
    super.onCleared()
    _connectedThreads.value?.run {
      forEach { it.cancel() }
    }
  }

  override fun onBluetoothRead(data: ByteArray) {
    val msgString = String(data, StandardCharsets.UTF_8)

    if (Formatter.removeNonAlphaCharacters(msgString) == "IIIII") {
      showMessage(stringResourceProvider.getString(R.string.unstable_weight))
    } else {
      updateWeightProduct(data)
    }
  }

  override fun onBluetoothConnecting() {
    showMessage(stringResourceProvider.getString(R.string.connecting))
  }

  override fun onBluetoothConnected(device: ConnectedThread) {
    _connectedThreads.value?.add(device)
    if (_connectedThreads.value?.size == 1) {
      val firstDevice = 0
      setActiveThread(firstDevice)
    }
    val scaleNumber = connectedThreads.value?.size

    showMessage(
      stringResourceProvider.getString(R.string.message_scale_connected)
    )
  }

  override fun onBluetoothError(message: String) {
    showMessage(message)
  }
}

