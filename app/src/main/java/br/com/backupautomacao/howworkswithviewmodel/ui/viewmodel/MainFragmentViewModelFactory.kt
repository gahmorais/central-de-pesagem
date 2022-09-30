package br.com.backupautomacao.howworkswithviewmodel.ui.viewmodel

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.howworkswithviewmodel.utils.Preferences


class MainFragmentViewModelFactory(private val preferences: Preferences, private val bluetoothAdapter: BluetoothAdapter) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return MainFragmentViewModel(preferences, bluetoothAdapter) as T
  }
}