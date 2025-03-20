package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.PrintWorker
import br.com.backupautomacao.centraldepesagem.PrintWorker.Companion.USB_PRINTER_NAME
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileNotFoundException
import java.security.InvalidParameterException

class MassProductionViewModel(
  private val configRepository: ConfigRepository,
  private val workManager: WorkManager,
  private val label: Label
) : ViewModel() {
  private val _mgvProduct = MutableStateFlow<MgvProduct?>(null)
  val mgvProduct = _mgvProduct.asStateFlow()

  private val retrofitInstance by lazy {
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    MgvApi.retrofitService(serverAddress)
  }

  val statusPrint = workManager.getWorkInfosForUniqueWorkFlow(PrintWorker.PRINT_TASK)

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

        mgvError?.let { error ->
          Timber.i("fetchItem: $error")
          return@launch
        }

        Timber.i("fetchItem: $item")
        _mgvProduct.update { item }

      } catch (e: IllegalArgumentException) {
        Timber.i("fetchItem: Endereço do servidor inválido")
      } catch (e: Exception) {
        Timber.i("fetchItem: Erro ao fazer requisição - ${e.message}")
      }
    }
  }

  fun updateWeight(weight: Double) {
    _mgvProduct.value?.weight = weight
  }

  fun print(quantity: String) {
    try {
      val product: MgvProduct = mgvProduct.value ?: MgvProduct()

      val newLabel =
        label.generateLabelWithQuantity(product, "etiqueta_base.prn", quantity.toInt())

      if (configRepository.getPrinterType() == PrinterType.USB) {
        Timber.i("Impressora USB selecionada")
      } else {
        val printerIp: String? = configRepository.getConfig(ConfigRepository.PRINTER_IP)
        val printerPort: Int = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
        val usbPrinterName: String? = configRepository.getConfig(USB_PRINTER_NAME)
        val printerType = configRepository.getPrinterType()
        val printTask = PrintWorker.createUniquePrintTask(
          printerType = printerType,
          printerAddress = printerIp,
          printerPort = printerPort,
          label = newLabel,
          usbPrinterName = usbPrinterName
        )
        workManager.enqueueUniqueWork(
          PrintWorker.PRINT_TASK,
          ExistingWorkPolicy.REPLACE,
          printTask
        )
      }

      _mgvProduct.update { MgvProduct() }
    } catch (e: InvalidParameterException) {
      Timber.e("Porta ou endereço IP inválidos: ${e.message}")
    } catch (e: FileNotFoundException) {
      Timber.e("Arquivo não encontrado: ${e.message}")
    } catch (e: Exception) {
      Timber.e("Erro inesperado ao imprimir: ${e.message}", e)
    }
  }
}