package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MassProductionViewModel(
  private val configRepository: ConfigRepository,
  private val workManager: WorkManager

) : ViewModel() {
  private val _mgvProduct = MutableLiveData<MgvProduct>()
  val mgvProduct: LiveData<MgvProduct> = _mgvProduct

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

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val (mgvError, item) = retrofitInstance.queryItem(
          item = code,
          palavraChave = mgvPassword,
          numeroLoja = storeNumber
        )

        mgvError?.let { error ->
          Log.i("MainFragmentViewModel", "fetchItem: $error")
        }
        item?.let { product ->
          _mgvProduct.value = product
        }

      } catch (e: IllegalArgumentException) {
        Log.i("MassProductionViewModel", "fetchItem: Endereço do servidor inválido")
      } catch (e: Exception) {
        Log.i("MassProductionViewModel", "fetchItem: Erro ao fazer requisição")
      }
    }

  }

  fun print() {
    TODO(
      "Verificar se a função print não pode virar uma classe para ser " +
          "reutilizada"
    )
  }
}