package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.com.backup.bluetooth.BluetoothConnected
import br.com.backup.bluetooth.BluetoothHandler
import br.com.backup.bluetooth.exceptions.ClosedSocketException
import br.com.backupautomacao.pesagem_modular.Constants
import br.com.backupautomacao.pesagem_modular.PrintWorker
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.data.model.Operation
import br.com.backupautomacao.pesagem_modular.data.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.data.repository.OperationRepository
import br.com.backupautomacao.pesagem_modular.labels.LabelBuilder
import br.com.backupautomacao.pesagem_modular.models.MgvProductImpl
import br.com.backupautomacao.pesagem_modular.network.IApiMgv
import br.com.backupautomacao.pesagem_modular.utils.Formatter
import br.com.backupautomacao.pesagem_modular.utils.StringResourceProvider
import br.com.backupautomacao.pesagem_modular.utils.properties
import br.com.backupautomacao.pesagem_modular.utils.removeNonAlphaCharacters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class MainViewModel(
  private val configRepository: ConfigRepository,
  private val operationRepository: OperationRepository,
  private val workManager: WorkManager,
  private val btHandler: BluetoothHandler,
  private val labelBuild: LabelBuilder,
  private val stringResourceProvider: StringResourceProvider,
  private val apiMgv: IApiMgv
) : ViewModel() {

  private val _labelsList = MutableStateFlow<List<String>>(listOf())
  val labelsList = _labelsList.asStateFlow()

  private val _labelType = MutableStateFlow("")
  val isFixed = MutableStateFlow(false)

  private val _mgvProductImpl = MutableStateFlow<MgvProductImpl?>(null)
  val mgvProductImpl = _mgvProductImpl.asStateFlow()

  private val _statusMessage = MutableSharedFlow<String>()
  val statusMessage = _statusMessage.asSharedFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading = _isLoading.asStateFlow()

  private val _activeDevice = MutableStateFlow<BluetoothConnected?>(null)
  val activeDevice = _activeDevice.asStateFlow()

  private val _devices = MutableStateFlow<List<BluetoothConnected>>(listOf())
  private val _updateStatus = MutableStateFlow(StatusApp.INICIO)

  init {
    clearAllWorks()
    updateLabelList()
    _updateStatus.onEach { status ->
      val automaticPrint = configRepository.getAutomaticPrint()
      when (status) {
        StatusApp.INICIO -> Log.i("MainFragmentViewModel", "Inicio: ")
        StatusApp.BUSCAR_PRODUTO -> Log.i("MainFragmentViewModel", "Buscando produto")
        StatusApp.GRAVAR_OPERACAO -> saveOperation()
        StatusApp.REALIZAR_PESAGEM -> _activeDevice.value?.let { weighing(it) }
        StatusApp.REALIZAR_IMPRESSAO -> if (automaticPrint) print()
        else -> Log.i("MainFragmentViewModel", "Processo finalizado")
      }
    }.launchIn(viewModelScope)
  }

  private fun clearAllWorks() {
    workManager.pruneWork()
  }

  private fun updateLabelList() {

    _labelsList.value = labelBuild.listLabels()
    _labelsList.value.let {
      if (it.isNotEmpty()) {
        _labelType.value = "verdemar"
      }
    }
  }

  fun updateLabelType(label: String) {
    _labelType.value = label
  }

  private suspend fun updateWeightProduct(message: ByteArray) {

    val newWeight: Double = Formatter.formatFromToledoProtocolP05(message)
    val priceForKg = _mgvProductImpl.value?.price ?: 0.0
    _mgvProductImpl.update {
      it?.copy(
        weight = newWeight,
        totalPrice = priceForKg.times(newWeight)
      )
    }

    delay(100)
    loadPrice(priceForKg)

    Log.i("MainViewModel", "updateWeightProduct:Product ${_mgvProductImpl.value}")
    //Só grava a operação se o peso for maior que zero
    if (newWeight > 0.0) {
      _updateStatus.value = StatusApp.GRAVAR_OPERACAO
    }
  }

  suspend fun fetchItem(codItem: String) {
    Log.i("MainFragmentViewModel", "fetchItem: Buscando item")

    if (codItem.isEmpty()) {
      showMessage(stringResourceProvider.getString(R.string.barcode_error))
      return
    }

    _updateStatus.value = StatusApp.BUSCAR_PRODUTO
    try {
      val mgvPassword = configRepository.getServerPassword()
      val storeNumber = configRepository.getStoreNumber()
      val product = apiMgv.queryItem(mgvPassword, storeNumber, codItem)
      _mgvProductImpl.emit(product)
      _updateStatus.value = StatusApp.REALIZAR_PESAGEM
    } catch (e: NoRouteToHostException) {
      _statusMessage.emit(stringResourceProvider.getString(R.string.invalid_address_server))
      Log.e("MainFragmentViewModel", "fetchItem: endereço inválido", e)
    } catch (e: ConnectException) {
      _statusMessage.emit(stringResourceProvider.getString(R.string.fail_to_connect_server))
    } catch (e: Exception) {
      e.message?.let {
        _statusMessage.emit(it)
        Log.e("MainFragmentViewModel", "fetchItem: ", e)
      }
    }

  }

  fun weighing(btConnected: BluetoothConnected) {
    try {
      Log.i("MainFragmentViewModel", "weighing: Realizando pesagem")
      if (checkProductIsNullOrEmpty()) {
        showMessage(stringResourceProvider.getString(R.string.error_get_product))
        return
      }
      val command = byteArrayOf(0x05)
      btConnected.write(command)
    } catch (e: ClosedSocketException) {
      e.message?.let { showMessage(it) }
    } catch (e: Exception) {
      e.message?.let { showMessage(it) }
    }
  }

  suspend fun print() {

    if (checkProductIsNullOrEmpty()) {
      showMessage(stringResourceProvider.getString(R.string.error_to_get_product))
      return
    }
    val product: MgvProductImpl? = _mgvProductImpl.value
    if ((product?.weight == null) || (product.weight == 0.0)) {
      showMessage(stringResourceProvider.getString(R.string.make_weighing))
      return
    }

    try {
      val printer = configRepository.getPrinterConfig()
      val label = labelBuild.build(_labelType.value)
      val newLabel = label.generateLabel(product).properties(1)

      val printTask = PrintWorker.createUniquePrintTask(
        printerAddress = printer.address,
        printerPort = printer.port,
        label = newLabel
      )

      workManager.enqueueUniqueWork(PrintWorker.PRINT_TASK, ExistingWorkPolicy.REPLACE, printTask)
      _updateStatus.value = StatusApp.FINALIZAR
      if (!isFixed.value) {
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

  fun showMessage(message: String = "") {
    viewModelScope.launch { _statusMessage.emit(message) }
  }

  private fun loadPrice(value: Double) {
    val priceByteArray = Formatter.formatDataToLoadPriceToledo(value)
    _activeDevice.value?.write(priceByteArray)
  }

  private fun clearFields() {
    _mgvProductImpl.value = null
  }

  fun monitoringPrintStatus(lifecycleOwner: LifecycleOwner) = workManager
    .getWorkInfosForUniqueWorkLiveData(PrintWorker.PRINT_TASK)
    .observe(lifecycleOwner) { worksInfo ->
      if (worksInfo.isNotEmpty()) {
        val workInfo = worksInfo[0]
        when (workInfo.state) {
          WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> _isLoading.value = true
          WorkInfo.State.SUCCEEDED -> {
            showMessage("Impressão finalizada com sucesso")
            _isLoading.value = false
          }

          WorkInfo.State.FAILED -> {
            showMessage("Impressão falhou")
            _isLoading.value = false
          }

          WorkInfo.State.BLOCKED -> {
            showMessage("Impressão bloqueada")
            _isLoading.value = false
          }

          WorkInfo.State.CANCELLED -> {
            showMessage("Impressão cancelada")
            _isLoading.value = false
          }
        }
      }
    }

  fun cancelPrint() {
    workManager.cancelUniqueWork(PrintWorker.PRINT_TASK)
  }

  private fun startCollectData(deviceConnected: BluetoothConnected) = deviceConnected
    .read().onEach { bytes ->
      val rawData = String(bytes, StandardCharsets.UTF_8)
      Log.i("MainFragmentViewModel", "startCollectData: Mostrando dados $rawData")
      when (val data = rawData.removeNonAlphaCharacters()) {
        Constants.UNSTABLE_WEIGHT -> showMessage("Peso instável, faça a pesagem novamente")
        Constants.NEGATIVE_WEIGHT -> showMessage("Peso negativo, faça a pesagem novamente")
        Constants.OVERWEIGHT -> showMessage("Balança em sobrecarga, remove o peso extra")
        else -> {
          if (data.isNotEmpty()) {
            updateWeightProduct(bytes)
          }
        }
      }
    }.launchIn(viewModelScope)

  suspend fun connectToDevices(btAdapter: BluetoothAdapter) {
    val devices = btHandler.getAvailableDevices(adapter = btAdapter)
    val newDevices = mutableListOf<BluetoothConnected>()
    showMessage("Buscando dispositivos disponíveis")

    devices.forEach { device ->
      val socket = try {
        withContext(Dispatchers.IO) {
          btHandler.connectToDevice(device)
        }
      } catch (e: Exception) {
        showMessage("Erro ao conectar com dispositivo")
        return
      }
      showMessage("Conectando ao dispositivo ${newDevices.size + 1}")
      newDevices.add(socket)
      startCollectData(socket)
      showMessage("Dispositivo conectado")
    }

    if (newDevices.size == 1) {
      _activeDevice.value = newDevices[0]
    }

    _devices.value = newDevices.toList()

  }

  private fun saveOperation() {
    try {
      val product = _mgvProductImpl.value
        ?: throw Exception("Não foi possível salvar a operação!\nProduto nulo")

      val operation = Operation(
        weight = product.weight,
        totalPrice = product.totalPrice,
        pricePerKilogram = product.price,
        username = Build.ID,
        description = product.description
      )

      viewModelScope.launch(Dispatchers.IO) {
        operationRepository.save(operation)
      }

      _updateStatus.value = StatusApp.REALIZAR_IMPRESSAO

    } catch (e: Exception) {
      Log.e("MainFragmentViewModel", "saveOperation: Erro ao salvar operação", e)
      showMessage(e.message!!)
    }
  }
}

