package br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory

import android.bluetooth.BluetoothAdapter
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel
import net.gotev.speech.Speech


class MainViewModelFactory(
  private val configRepository: ConfigRepository,
  private val bluetoothAdapter: BluetoothAdapter,
  private val speech: Speech
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return MainFragmentViewModel(configRepository, bluetoothAdapter, speech) as T
  }
}