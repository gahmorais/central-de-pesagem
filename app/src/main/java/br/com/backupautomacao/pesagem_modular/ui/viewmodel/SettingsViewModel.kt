package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository.Companion.KEY_LABEL_TEMPLATE_KG
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository.Companion.KEY_LABEL_TEMPLATE_QUANT
import br.com.backupautomacao.pesagem_modular.utils.PrinterType
import br.com.backupautomacao.pesagem_modular.utils.ResourceProvider
import br.com.backupautomacao.pesagem_modular.utils.Validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
  private val configRepository: ConfigRepository,
  private val resourceProvider: ResourceProvider
) : ViewModel() {

  private val _statusMessage = MutableStateFlow("")
  val statusMessage: StateFlow<String> = _statusMessage

  private val _printerTypeLiveData = MutableStateFlow(
    configRepository.getTextConfig(ConfigRepository.PRINTER_TYPE)
  )
  val printerTypeLiveData: StateFlow<String> = _printerTypeLiveData

  private val _selectedLabelTemplate = MutableStateFlow(
    configRepository.getTextConfig(ConfigRepository.LABEL_TEMPLATE_NAME)
  )
  val selectedLabelTemplate: StateFlow<String> = _selectedLabelTemplate

  private val _labelTemplateKg = MutableStateFlow(
    configRepository.getTextConfig(KEY_LABEL_TEMPLATE_KG)
  )
  val labelTemplateKg = _labelTemplateKg.asStateFlow()

  private val _labelTemplateQuant =
    MutableStateFlow(configRepository.getTextConfig(KEY_LABEL_TEMPLATE_QUANT))
  val labelTemplateQuant = _labelTemplateQuant.asStateFlow()

  private val _serverAddress = MutableStateFlow(
    configRepository.getTextConfig(ConfigRepository.SERVER_ADDRESS)
  )
  val serverAddress: StateFlow<String> = _serverAddress

  private val _printerIp = MutableStateFlow(
    configRepository.getTextConfig(ConfigRepository.PRINTER_IP)
  )
  val printerIp: StateFlow<String> = _printerIp

  private val _printerPort = MutableStateFlow(
    configRepository.getIntConfig(ConfigRepository.PRINTER_PORT).toString()
  )
  val printerPort: StateFlow<String> = _printerPort

  private val _storeNumber = MutableStateFlow(
    configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()
  )
  val storeNumber: StateFlow<String> = _storeNumber

  private val _passwordApi = MutableStateFlow(
    configRepository.getTextConfig(ConfigRepository.PASSWORD_API)
  )
  val passwordMgv: StateFlow<String> = _passwordApi

  var automaticPrintingEnable: Boolean =
    configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)

  fun saveNetworkConfig(address: String, port: String) {
    if (!Validate.validaIp(address)) {
      _statusMessage.value = resourceProvider.getString(R.string.ip_not_setted)
      return
    }

    if (!Validate.validaPorta(port)) {
      _statusMessage.value = resourceProvider.getString(R.string.invalid_port)
      return
    }

    val portAddress = port.toIntOrNull()
    if (portAddress == null) {
      _statusMessage.value = resourceProvider.getString(R.string.invalid_port)
      return
    }

    configRepository.setTextConfig(ConfigRepository.PRINTER_IP, address)
    configRepository.setIntConfig(ConfigRepository.PRINTER_PORT, portAddress)

    _printerIp.value = address
    _printerPort.value = port
    _statusMessage.value = resourceProvider.getString(R.string.save_printer_success)
  }

  fun saveApiConfig(password: String, store: String) {
    when {
      password.isEmpty() -> {
        _statusMessage.value = resourceProvider.getString(R.string.fill_keyword_field)
        return
      }

      store.isEmpty() -> {
        _statusMessage.value = resourceProvider.getString(R.string.invalid_store_number)
        return
      }
    }

    val storeNumberInt = store.toIntOrNull()
    if (storeNumberInt == null) {
      _statusMessage.value = resourceProvider.getString(R.string.invalid_store_number)
      return
    }

    configRepository.setTextConfig(ConfigRepository.PASSWORD_API, password)
    configRepository.setIntConfig(ConfigRepository.STORE_NUMBER, storeNumberInt)

    _passwordApi.value = password
    _storeNumber.value = store
    _statusMessage.value = resourceProvider.getString(R.string.sucessfully_save_mgv_config)
  }

  fun saveServerAddress(address: String) {
    if (address.isEmpty()) {
      _statusMessage.value = resourceProvider.getString(R.string.invalid_address_server)
      return
    }

    configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, address)
    _serverAddress.value = address
    _statusMessage.value = resourceProvider.getString(R.string.sucessfully_save_server_address)
    _statusMessage.value = resourceProvider.getString(R.string.restart_app)

  }

  fun enableAutomaticPrint(isEnable: Boolean) {
    val currentValue = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
    if (isEnable != currentValue) {
      automaticPrintingEnable = isEnable
      configRepository.setBooleanConfig(ConfigRepository.AUTOMATIC_PRINT, isEnable)
      _statusMessage.value =
        if (isEnable) resourceProvider.getString(R.string.automatic_print_enable)
        else resourceProvider.getString(R.string.automatic_print_disable)
    }
  }

  fun savePrinterUsbName(model: String, deviceName: String) {
    configRepository.setUsbConfig(model, deviceName)
  }

  fun savePrinterConfig(printerType: PrinterType) {
    configRepository.setTextConfig(ConfigRepository.PRINTER_TYPE, printerType.toString())
    _printerTypeLiveData.value = printerType.toString()
    _statusMessage.value = resourceProvider.getString(R.string.sucessfully_save_printer_type)
  }

  fun clearStatusMessage() {
    _statusMessage.value = ""
  }

  fun setLabelTemplateKg(template: String) {
    configRepository.setTextConfig(KEY_LABEL_TEMPLATE_KG, template)
  }

  fun setLabelTemplateQuant(template: String) {
    configRepository.setTextConfig(KEY_LABEL_TEMPLATE_KG, template)
  }
}