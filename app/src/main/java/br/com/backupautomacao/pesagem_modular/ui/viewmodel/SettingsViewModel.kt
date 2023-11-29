package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.utils.StringResourceProvider
import br.com.backupautomacao.pesagem_modular.utils.Validate
import org.koin.core.component.KoinComponent

class SettingsViewModel(
  private val configRepository: ConfigRepository,
  private val stringResourceProvider: StringResourceProvider
) : ViewModel(), KoinComponent {
  private val _statusMessage = MutableLiveData<String>()
  var automaticPrintingEnable: Boolean =
    configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
  val statusMessage = _statusMessage
  fun savePrinterAddress(address: String, port: String) {
    if (!Validate.validaIp(address)) {
      _statusMessage.value = stringResourceProvider.getString(R.string.ip_not_setted)
      return
    }

    if (!Validate.validaPorta(port)) {
      _statusMessage.value = stringResourceProvider.getString(R.string.invalid_port)
      return
    }

    val portAddress = Integer.parseInt(port)

    configRepository.setTextConfig(ConfigRepository.PRINTER_IP, address)
    configRepository.setIntConfig(ConfigRepository.PRINTER_PORT, portAddress)
    _statusMessage.value = stringResourceProvider.getString(R.string.save_printer_success)
  }

  fun saveMgvConfig(password: String, store: String) {
    if (password.isEmpty()) {
      _statusMessage.value = stringResourceProvider.getString(R.string.fill_keyword_field)
      return
    } else if (password.length < 8) {
      _statusMessage.value =
        stringResourceProvider.getString(R.string.stronger_password_error)
      return
    }

    if (store.isEmpty()) {
      _statusMessage.value =
        stringResourceProvider.getString(R.string.invalid_store_number)
      return
    }

    configRepository.setTextConfig(ConfigRepository.PASSWORD_MGV, password)
    configRepository.setIntConfig(ConfigRepository.STORE_NUMBER, Integer.parseInt(store))
    _statusMessage.value =
      stringResourceProvider.getString(R.string.sucessfully_save_mgv_config)

  }

  fun saveServerAddress(address: String) {
    if (address.isEmpty()) {
      _statusMessage.value =
        stringResourceProvider.getString(R.string.invalid_address_server)
      return
    }

    configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, address)
    _statusMessage.value =
      stringResourceProvider.getString(R.string.sucessfully_save_server_address)
  }

  fun enableAutomaticPrint(isEnable: Boolean) {
    val currentValue = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
    if (isEnable != currentValue) {
      automaticPrintingEnable = isEnable
      configRepository.setBooleanConfig(ConfigRepository.AUTOMATIC_PRINT, isEnable)
      _statusMessage.value =
        if (isEnable) stringResourceProvider.getString(R.string.automatic_print_enable) else stringResourceProvider.getString(
          R.string.automatic_print_disable
        )
    }
  }
}