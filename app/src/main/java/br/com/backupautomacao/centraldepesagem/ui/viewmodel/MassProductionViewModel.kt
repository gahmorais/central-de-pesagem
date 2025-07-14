package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.workers.NetworkWorker
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import br.com.backupautomacao.centraldepesagem.utils.UnitOfMeasure
import br.com.backupautomacao.centraldepesagem.workers.PRINT_TASK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileNotFoundException
import java.security.InvalidParameterException

enum class InputMode {
  QUANTITY, WEIGHT
}

class MassProductionViewModel(
  private val configRepository: ConfigRepository,
  private val workManager: WorkManager,
  private val label: Label
) : ViewModel() {

  private val _mgvProduct = MutableStateFlow<MgvProduct?>(null)
  val mgvProduct = _mgvProduct.asStateFlow()

  private val _inputMode = MutableStateFlow(InputMode.QUANTITY)
  val inputMode = _inputMode.asStateFlow()

  val statusPrint = workManager.getWorkInfosForUniqueWorkFlow(PRINT_TASK)

  private val retrofitInstance by lazy {
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    MgvApi.retrofitService(serverAddress)
  }

  fun setMode(mode: InputMode) {
    _inputMode.value = mode
  }

  fun toggleMode() {
    _inputMode.update {
      if (it == InputMode.QUANTITY) InputMode.WEIGHT else InputMode.QUANTITY
    }
  }

  fun shouldRequireWeight(): Boolean {
    return _inputMode.value == InputMode.WEIGHT
  }

  fun updateWeight(weight: Double) {
    _mgvProduct.value?.weight = weight
  }

  fun fetchItem(code: String) {
    val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
    val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()

    if (code.isEmpty()) {
      Timber.i("fetchItem: Por favor digite o código do produto")
      return
    }

    viewModelScope.launch {
      try {
        val (mgvError, item) = retrofitInstance.queryItem(
          item = code,
          palavraChave = mgvPassword,
          numeroLoja = storeNumber
        )

        mgvError?.let {
          Timber.i("fetchItem: $it")
          return@launch
        }

        Timber.i("fetchItem: $item")
        _mgvProduct.value = item

      } catch (e: IllegalArgumentException) {
        Timber.i("fetchItem: Endereço do servidor inválido")
      } catch (e: Exception) {
        Timber.i("fetchItem: Erro ao fazer requisição - ${e.message}")
      }
    }
  }

  fun print(quantity: String) {
    try {
      val product = _mgvProduct.value ?: MgvProduct()

      val newLabel = label.generateLabelWithQuantity(product, "etiqueta_base.prn", quantity.toInt())

      if (configRepository.getPrinterType() == PrinterType.USB) {
        Timber.i("Impressora USB selecionada")
        // Aqui poderia adicionar UsbWorker se necessário
      } else {
        val printerIp = configRepository.getConfig(ConfigRepository.PRINTER_IP)
        val printerPort = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)

        val printTask = NetworkWorker.createUniquePrintTask(
          printerAddress = printerIp,
          printerPort = printerPort,
          label = newLabel
        )

        workManager.enqueueUniqueWork(
          PRINT_TASK,
          ExistingWorkPolicy.REPLACE,
          printTask
        )
      }

      _mgvProduct.value = MgvProduct()

    } catch (e: InvalidParameterException) {
      Timber.e("Porta ou endereço IP inválidos: ${e.message}")
    } catch (e: FileNotFoundException) {
      Timber.e("Arquivo não encontrado: ${e.message}")
    } catch (e: Exception) {
      Timber.e("Erro inesperado ao imprimir: ${e.message}", e)
    }
  }
}