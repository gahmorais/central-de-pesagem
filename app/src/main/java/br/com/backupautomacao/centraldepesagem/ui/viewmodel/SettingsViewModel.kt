package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider
import br.com.backupautomacao.centraldepesagem.utils.Validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val configRepository: ConfigRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    var automaticPrintingEnable: Boolean =
        configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)

    private val _printerTypeLiveData = MutableStateFlow("")

    init {
        _printerTypeLiveData.update {
            configRepository.getTextConfig(ConfigRepository.PRINTER_TYPE) ?: "Network"
        }
    }

    fun saveNetworkConfig(address: String, port: String) {
        if (!Validate.validaIp(address)) {
            _statusMessage.value =
                resourceProvider.getString(R.string.ip_not_setted)
            return
        }

        if (!Validate.validaPorta(port)) {
            _statusMessage.value = resourceProvider.getString(R.string.invalid_port)
            return
        }

        val portAddress = Integer.parseInt(port)

        configRepository.setTextConfig(ConfigRepository.PRINTER_IP, address)
        configRepository.setIntConfig(ConfigRepository.PRINTER_PORT, portAddress)
        _statusMessage.value =
            resourceProvider.getString(R.string.save_printer_success)
    }

    fun saveMgvConfig(password: String, store: String) {
        if (password.isEmpty()) {
            _statusMessage.value =
                resourceProvider.getString(R.string.fill_keyword_field)
            return
        } else if (password.length < 8) {
            _statusMessage.value =
                resourceProvider.getString(R.string.stronger_password_error)
            return
        }

        if (store.isEmpty()) {
            _statusMessage.value =
                resourceProvider.getString(R.string.invalid_store_number)
            return
        }

        configRepository.setTextConfig(ConfigRepository.PASSWORD_MGV, password)
        configRepository.setIntConfig(ConfigRepository.STORE_NUMBER, Integer.parseInt(store))
        _statusMessage.value =
            resourceProvider.getString(R.string.sucessfully_save_mgv_config)
    }

    fun saveServerAddress(address: String) {
        if (address.isEmpty()) {
            _statusMessage.value =
                resourceProvider.getString(R.string.invalid_address_server)
            return
        }

        configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, address)
        _statusMessage.value =
            resourceProvider.getString(R.string.sucessfully_save_server_address)
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

    fun savePrinterUsbName(printerName: String) {
        configRepository.setUsbConfig(ConfigRepository.USB_PRINTER_NAME, printerName)
    }

    fun savePrinterConfig(printerType: PrinterType) {
        configRepository.setPrinterType(printerType.toString())
        when (printerType) {
            PrinterType.NETWORK -> {}
            PrinterType.USB -> {}
        }
        _statusMessage.value =
            resourceProvider.getString(R.string.sucessfully_save_printer_type)
    }

    fun clearStatusMessage() {
        _statusMessage.value = ""
    }


}