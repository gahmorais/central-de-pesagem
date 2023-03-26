package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.utils.Validate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
  private val configRepository:ConfigRepository by inject()
  private val _statusMessage = MutableLiveData<String>()
  var automaticPrintingEnable: Boolean =
    configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
  val statusMessage = _statusMessage
  fun savePrinterAddress(address: String, port: String) {
    if (!Validate.validaIp(address)) {
      _statusMessage.value = getApplication<AppApplication>().getString(R.string.ip_not_setted)
      return
    }

    if (!Validate.validaPorta(port)) {
      _statusMessage.value = getApplication<AppApplication>().getString(R.string.invalid_port)
      return
    }

    val portAddress = Integer.parseInt(port)

    configRepository.setTextConfig(ConfigRepository.PRINTER_IP, address)
    configRepository.setIntConfig(ConfigRepository.PRINTER_PORT, portAddress)
    _statusMessage.value = getApplication<AppApplication>().getString(R.string.save_printer_success)
  }

  fun saveMgvConfig(password: String, store: String) {
    if (password.isEmpty()) {
      _statusMessage.value =  getApplication<AppApplication>().getString(R.string.fill_keyword_field)
      return
    } else if (password.length < 8) {
      _statusMessage.value =  getApplication<AppApplication>().getString(R.string.stronger_password_error)
      return
    }

    if (store.isEmpty()) {
      _statusMessage.value = getApplication<AppApplication>().getString(R.string.invalid_store_number)
      return
    }

    configRepository.setTextConfig(ConfigRepository.PASSWORD_MGV, password)
    configRepository.setIntConfig(ConfigRepository.STORE_NUMBER, Integer.parseInt(store))
    _statusMessage.value =  getApplication<AppApplication>().getString(R.string.sucessfully_save_mgv_config)

  }

  fun saveServerAddress(address: String) {
    if (address.isEmpty()) {
      _statusMessage.value = getApplication<AppApplication>().getString(R.string.invalid_address_server)
      return
    }

    configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, address)
    _statusMessage.value = getApplication<AppApplication>().getString(R.string.sucessfully_save_server_address)
  }

  fun enableAutomaticPrint(isEnable: Boolean) {
    val currentValue = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
    if (isEnable != currentValue) {
      automaticPrintingEnable = isEnable
      configRepository.setBooleanConfig(ConfigRepository.AUTOMATIC_PRINT, isEnable)
      _statusMessage.value =
        if (isEnable) getApplication<AppApplication>().getString(R.string.automatic_print_enable) else getApplication<AppApplication>().getString(R.string.automatic_print_disable)
    }
  }
}