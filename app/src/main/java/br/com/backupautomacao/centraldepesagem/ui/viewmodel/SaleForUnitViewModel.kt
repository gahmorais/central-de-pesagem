package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.PrintWorker
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.labels.LabelBuilder
import br.com.backupautomacao.centraldepesagem.labels.LabelBuilder.Companion.properties
import br.com.backupautomacao.centraldepesagem.models.MgvProductImpl
import br.com.backupautomacao.centraldepesagem.network.MgvRepository
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.FileNotFoundException
import java.security.InvalidParameterException

class SaleForUnitViewModel(application: Application) : AndroidViewModel(application),
  KoinComponent {
  private val _mgvProduct = MutableLiveData<MgvProductImpl?>()
  val mgvProduct: LiveData<MgvProductImpl?> = _mgvProduct

  private val configRepository: ConfigRepository by inject()
  private val workManager: WorkManager by inject()

  fun fetchItem(code: String) {

    if (code.isEmpty()) {
      Log.i("MassProductionViewModel", "fetchItem: Por favor digite o código do produto")
      return
    }

    viewModelScope.launch {
      try {
        val mgvRepository = MgvRepository()
        val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
        val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()
        val product = mgvRepository.queryItem(mgvPassword, storeNumber, code)

        product?.codNutrictionFacts?.let {
          product.nutrictionFacts = mgvRepository.getNutriction(mgvPassword, storeNumber, it)
        }

        product?.codExtraInfo?.let {
          product.extraInfo = mgvRepository.getExtraInfo(mgvPassword, storeNumber, it)
        }

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

  fun print(quantity: String) {
    try {
      val product: MgvProductImpl = mgvProduct.value ?: throw Exception("Produto nulo")
      val label = LabelBuilder
        .build(
          "ET313",
          getApplication()
        )

      val newLabel = label
        .generateLabel(product)
        .properties(copies = quantity.toInt())

      val printerIp: String? = configRepository.getConfig(ConfigRepository.PRINTER_IP)
      val printerPort: Int = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
      val printTask = PrintWorker.createUniquePrintTask(
        printerAddress = printerIp,
        printerPort = printerPort,
        label = newLabel
      )

      workManager.enqueueUniqueWork(PrintWorker.PRINT_TASK, ExistingWorkPolicy.REPLACE, printTask)
//      _mgvProduct.value = null
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