package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.backupautomacao.centraldepesagem.Constants
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.utils.Validate

class SettingsViewModel(private val configRepository: ConfigRepository) : ViewModel() {
  private val _statusMessage = MutableLiveData<String>()
  var automaticPrintingEnable: Boolean =
    configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
  val statusMessage = _statusMessage
  fun savePrinterAddress(address: String, port: String) {
    if (!Validate.validaIp(address)) {
      _statusMessage.value = Constants.IP_NOT_SET_ERROR
      return
    }

    if (!Validate.validaPorta(port)) {
      _statusMessage.value = Constants.PORT_ERROR
      return
    }

    val portAddress = Integer.parseInt(port)

    configRepository.setTextConfig(ConfigRepository.PRINTER_IP, address)
    configRepository.setIntConfig(ConfigRepository.PRINTER_PORT, portAddress)
    _statusMessage.value = Constants.SUCCESS_TO_SAVE_PRINTER_ADDRESS
  }

  fun saveMgvConfig(password: String, store: String) {
    if (password.isEmpty()) {
      _statusMessage.value = Constants.EMPTY_PASSWORD_ERROR
      return
    } else if (password.length < 8) {
      _statusMessage.value = Constants.STRONGER_PASSWORD_ERROR
      return
    }

    if (store.isEmpty()) {
      _statusMessage.value = Constants.INVALID_STORE_NUMBER_ERROR
      return
    }

    configRepository.setTextConfig(ConfigRepository.PASSWORD_MGV, password)
    configRepository.setIntConfig(ConfigRepository.STORE_NUMBER, Integer.parseInt(store))
    _statusMessage.value = Constants.SUCCESS_TO_SAVE_MGV_CONFIG

  }

  fun saveServerAddress(address: String) {
    if (address.isEmpty()) {
      _statusMessage.value = Constants.SERVER_ADDRESS_ERROR
      return
    }

    configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, address)
    _statusMessage.value = Constants.SUCESS_TO_SAVE_SERVER_ADDRESS
  }

  fun enableAutomaticPrint(isEnable: Boolean) {
    val currentValue = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
    if (isEnable != currentValue) {
      automaticPrintingEnable = isEnable
      configRepository.setBooleanConfig(ConfigRepository.AUTOMATIC_PRINT, isEnable)
      _statusMessage.value =
        if (isEnable) Constants.MESSAGE_AUTOMATIC_PRINT_ENABLE else Constants.MESSAGE_AUTOMATIC_PRINT_DISABLE
    }
  }
}