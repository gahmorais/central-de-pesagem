package br.com.backupautomacao.centraldepesagem.ui.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider

class SettingsViewModelFactory(
    private val configRepository: ConfigRepository,
    private val resourceProvider: ResourceProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(configRepository, resourceProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
