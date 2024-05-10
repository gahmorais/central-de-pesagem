package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.data.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.models.Printer
import br.com.backupautomacao.pesagem_modular.utils.StringResourceProvider
import br.com.backupautomacao.pesagem_modular.utils.Validate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
  private val configRepository: ConfigRepository,
  private val stringResourceProvider: StringResourceProvider
) : ViewModel() {
  private val _statusMessage = MutableLiveData<String>()
  private val _automaticPrintingEnable = MutableStateFlow(false)
  val automaticPrintingEnable = _automaticPrintingEnable.asStateFlow()

  val statusMessage = _statusMessage

  private val _printerConfig = MutableStateFlow(Printer("127.000.000.001", 9100))
  val printerConfig = _printerConfig.asStateFlow()

  private val _serverAddress = MutableStateFlow("")
  val serverAddress = _serverAddress.asStateFlow()

  private val _storeNumber = MutableStateFlow(1)
  val storeNumber = _storeNumber.asStateFlow()

  init {
    viewModelScope.launch {
      val printer = withContext(Dispatchers.IO) {
        configRepository.getPrinterConfig()
      }
      val automaticPrint = withContext(Dispatchers.IO) {
        configRepository.getAutomaticPrint()
      }

      _printerConfig.emit(printer)
      _automaticPrintingEnable.emit(automaticPrint)
    }
  }

  suspend fun savePrinterAddress(address: String, port: String) {
    if (!Validate.validaIp(address)) {
      _statusMessage.value = stringResourceProvider.getString(R.string.ip_not_setted)
      return
    }

    if (!Validate.validaPorta(port)) {
      _statusMessage.value = stringResourceProvider.getString(R.string.invalid_port)
      return
    }

    val portAddress = Integer.parseInt(port)

    configRepository.setPrinterConfig(address, portAddress)
    _statusMessage.value = stringResourceProvider.getString(R.string.save_printer_success)
  }

  suspend fun saveMgvConfig(password: String, store: String) {
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

    withContext(Dispatchers.IO) {
      configRepository.setServerPassword(password)
    }
    withContext(Dispatchers.IO) {
      configRepository.setStoreNumber(store.toInt())
    }
    _statusMessage.value = stringResourceProvider.getString(R.string.sucessfully_save_mgv_config)

  }

  suspend fun saveServerAddress(address: String) {
    if (address.isEmpty()) {
      _statusMessage.value =
        stringResourceProvider.getString(R.string.invalid_address_server)
      return
    }

    withContext(Dispatchers.IO){
      configRepository.setServerAddress(address)
    }

    _statusMessage.value =
      stringResourceProvider.getString(R.string.sucessfully_save_server_address)
  }

  suspend fun enableAutomaticPrint(isEnable: Boolean) {
    val currentValue = configRepository.getAutomaticPrint()
    if (isEnable != currentValue) {
      _automaticPrintingEnable.value = isEnable
      configRepository.setAutomaticPrint(isEnable)
      _statusMessage.value =
        if (isEnable) stringResourceProvider.getString(R.string.automatic_print_enable) else stringResourceProvider.getString(
          R.string.automatic_print_disable
        )
    }
  }
}