package br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel


class MainFragmentViewModelFactory(private val configRepository: ConfigRepository, private val bluetoothAdapter: BluetoothAdapter) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return MainFragmentViewModel(configRepository, bluetoothAdapter) as T
  }
}