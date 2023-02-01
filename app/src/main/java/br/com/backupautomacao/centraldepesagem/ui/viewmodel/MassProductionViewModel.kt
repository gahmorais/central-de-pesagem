package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MassProductionViewModel(application: Application) : AndroidViewModel(application) {
  private val _mgvProduct = MutableLiveData<MgvProduct?>()
  val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

  private val configRepository by lazy {
    getApplication<AppApplication>().configRepository
  }

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

  fun print() {
    Log.i("MassProductionViewModel", "print: ${_mgvProduct.value}")
  }
}