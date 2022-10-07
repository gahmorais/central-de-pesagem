package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.utils.Validate
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsViewModel(private val configRepository: ConfigRepository) : ViewModel() {
  private val _statusMessage = MutableLiveData<String>()
  var automaticPrintingEnable: Boolean = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
  val statusMessage = _statusMessage
  fun savePrinterAddress(address: String) {
    if (!Validate.validaIp(address)) {
      _statusMessage.value = "Ip inválido"
      return
    }
    configRepository.setTextConfig(ConfigRepository.PRINTER_IP, address)
    _statusMessage.value = "IP Salvo com sucesso"
  }

  fun enableAutomaticPrint(isEnable: Boolean) {
    val currentValue = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
    if (isEnable != currentValue) {
      automaticPrintingEnable = isEnable
      configRepository.setBooleanConfig(ConfigRepository.AUTOMATIC_PRINT, isEnable)
      _statusMessage.value =
        if (isEnable) "Impressão automática ativada" else "Impressão automática desativada"
    }
  }
}