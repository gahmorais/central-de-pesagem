package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository

class SettingsViewModelFactory(private val configRepository: ConfigRepository) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return SettingsViewModel(configRepository) as T
  }
}