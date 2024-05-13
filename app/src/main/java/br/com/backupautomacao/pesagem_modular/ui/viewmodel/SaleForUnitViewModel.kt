package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import br.com.backupautomacao.pesagem_modular.PrintWorker
import br.com.backupautomacao.pesagem_modular.data.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.labels.LabelBuilder
import br.com.backupautomacao.pesagem_modular.models.MgvProductImpl
import br.com.backupautomacao.pesagem_modular.network.IApiMgv
import br.com.backupautomacao.pesagem_modular.utils.properties
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.io.FileNotFoundException
import java.security.InvalidParameterException

class SaleForUnitViewModel(
  private val configRepository: ConfigRepository,
  private val workManager: WorkManager,
  private val labelBuilder: LabelBuilder,
  private val apiMgv: IApiMgv
) : ViewModel(),
  KoinComponent {
  private val _mgvProduct = MutableLiveData<MgvProductImpl?>()
  val mgvProduct: LiveData<MgvProductImpl?> = _mgvProduct


  fun fetchItem(code: String) {

    if (code.isEmpty()) {
      Log.i("MassProductionViewModel", "fetchItem: Por favor digite o código do produto")
      return
    }

    viewModelScope.launch {
      try {
        val mgvPassword = configRepository.getServerPassword()
        val storeNumber = configRepository.getStoreNumber()
        val product = apiMgv.queryItem(mgvPassword, storeNumber, code)
        _mgvProduct.value = product
      } catch (e: IllegalArgumentException) {
        Log.i("MassProductionViewModel", "fetchItem: Endereço do servidor inválido")
      } catch (e: Exception) {
        Log.i("MassProductionViewModel", "fetchItem: Erro ao fazer requisição - ${e.message}")
      }
    }

  }

  fun updateQuantity(quantity: Int) {
    _mgvProduct.value?.unit = quantity
  }

  suspend fun print(quantity: String) {
    try {
      val product: MgvProductImpl = mgvProduct.value ?: throw Exception("Produto nulo")
      val label = labelBuilder.build("ET313")

      val newLabel = label.generateLabel(product).properties(copies = quantity.toInt())
      val printer = configRepository.getPrinterConfig()
      val printTask = PrintWorker.createUniquePrintTask(
        printerAddress = printer.address,
        printerPort = printer.port,
        label = newLabel
      )

      workManager.enqueueUniqueWork(PrintWorker.PRINT_TASK, ExistingWorkPolicy.REPLACE, printTask)
    } catch (e: InvalidParameterException) {
      Log.i("MassProductionViewModel", "print: Porta ou endereço ip inválidos")
    } catch (e: FileNotFoundException) {
      println("Erro: ${e.message}")
      Log.i("MassProductionViewModel", "print: Arquivo não encontrado}")
    } catch (e: Exception) {
      Log.e("MainFragmentViewModel", "print: ${e.message}", e)
      e.printStackTrace()
    }
  }
}