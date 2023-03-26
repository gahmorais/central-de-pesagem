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
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Label
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.FileNotFoundException
import java.security.InvalidParameterException

class MassProductionViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
  private val _mgvProduct = MutableLiveData<MgvProduct?>()
  val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

  private val configRepository:ConfigRepository by inject()
  private val workManager:WorkManager by inject()

  private val retrofitInstance by lazy {
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    MgvApi.retrofitService(serverAddress)
  }

  fun fetchItem(code: String) {
    val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
    val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()

    if (code.isEmpty()) {
      Log.i("MassProductionViewModel", "fetchItem: Por favor digite o código do produto")
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
          Log.i("MainFragmentViewModel", "fetchItem: $error")
          return@launch
        }

        Log.i("MassProductionViewModel", "fetchItem: $item")
        _mgvProduct.value = item

      } catch (e: IllegalArgumentException) {
        Log.i("MassProductionViewModel", "fetchItem: Endereço do servidor inválido")
      } catch (e: Exception) {
        Log.i("MassProductionViewModel", "fetchItem: Erro ao fazer requisição - ${e.message}")
      }
    }

  }

  fun updateWeight(weight: Double) {
    _mgvProduct.value?.weight = weight
  }

  fun print(quantity: String) {
    try {
      val product: MgvProduct = mgvProduct.value ?: throw Exception("Produto nulo")
      val label = Label(getApplication())
      val newLabel =
        label.generateLabelWithQuantity(product, "labels/etiqueta_modelo_grande.prn", quantity.toInt())
      val printerIp: String? = configRepository.getConfig(ConfigRepository.PRINTER_IP)
      val printerPort: Int = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
      val printTask = PrintWorker.createUniquePrintTask(
        printerAddress = printerIp,
        printerPort = printerPort,
        label = newLabel
      )
      workManager.enqueueUniqueWork(PrintWorker.PRINT_TASK, ExistingWorkPolicy.REPLACE, printTask)
      _mgvProduct.value = null
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