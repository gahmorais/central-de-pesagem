package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository

class SettingsViewModel(private val configRepository: ConfigRepository) : ViewModel() {
  private val _statusMessage = MutableLiveData<String>()
  val statusMessage = _statusMessage
  fun savePrinterAddress(address: String) {
    configRepository.setConfiguracao(ConfigRepository.PRINTER_IP, address)
    _statusMessage.value = "IP Salvo com sucesso"
  }
}