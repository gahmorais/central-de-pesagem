package br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel
import net.gotev.speech.Speech

class MainViewModelFactory(private val application:AppApplication) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return MainFragmentViewModel(application) as T
  }
}