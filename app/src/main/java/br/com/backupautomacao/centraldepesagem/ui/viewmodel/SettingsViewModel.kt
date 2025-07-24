package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val configRepository: ConfigRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    companion object {
        private const val KEY_LABEL_TEMPLATE_KG = "LABEL_TEMPLATE_KG"
        private const val KEY_LABEL_TEMPLATE_QUANT = "LABEL_TEMPLATE_QUANT"
    }

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _printerTypeLiveData = MutableStateFlow("")
    val printerTypeLiveData: StateFlow<String> = _printerTypeLiveData

    private val _selectedLabelTemplate = MutableStateFlow("")
    val selectedLabelTemplate: StateFlow<String> = _selectedLabelTemplate

    private val _labelTemplateKg = MutableStateFlow("")
    val labelTemplateKg: StateFlow<String> = _labelTemplateKg

    private val _labelTemplateQuant = MutableStateFlow("")
    val labelTemplateQuant: StateFlow<String> = _labelTemplateQuant

    init {
        _printerTypeLiveData.value =
            configRepository.getTextConfig(ConfigRepository.PRINTER_TYPE) ?: ""

        _selectedLabelTemplate.value =
            configRepository.getTextConfig(ConfigRepository.LABEL_TEMPLATE_NAME) ?: ""

        _labelTemplateKg.value =
            configRepository.getTextConfig(KEY_LABEL_TEMPLATE_KG) ?: ""

        _labelTemplateQuant.value =
            configRepository.getTextConfig(KEY_LABEL_TEMPLATE_QUANT) ?: ""
    }

    fun savePrinterUsbName(model: String, deviceName: String) {
        configRepository.setUsbConfig(model, deviceName)
    }

    fun saveNetworkConfig(ip: String, port: String) {
        configRepository.setTextConfig(ConfigRepository.PRINTER_IP, ip)
        configRepository.setIntConfig(ConfigRepository.PRINTER_PORT, port.toIntOrNull() ?: 0)
    }

    fun savePrinterConfig(printerType: PrinterType) {
        configRepository.setTextConfig(ConfigRepository.PRINTER_TYPE, printerType.toString())
        _statusMessage.value = resourceProvider.getString(R.string.sucessfully_save_printer_type)
    }

    fun saveServerAddress(address: String) {
        configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, address)
    }

    fun saveMgvConfig(password: String, store: String) {
        configRepository.setTextConfig(ConfigRepository.PASSWORD_MGV, password)
        configRepository.setIntConfig(ConfigRepository.STORE_NUMBER, store.toIntOrNull() ?: 0)
    }

    fun enableAutomaticPrint(enabled: Boolean) {
        configRepository.setBooleanConfig(ConfigRepository.AUTOMATIC_PRINT, enabled)
    }

    fun setStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun clearStatusMessage() {
        _statusMessage.value = ""
    }

    fun setLabelTemplateKg(template: String) {
        _labelTemplateKg.value = template
        configRepository.setTextConfig(KEY_LABEL_TEMPLATE_KG, template)
    }

    fun setLabelTemplateQuant(template: String) {
        _labelTemplateQuant.value = template
        configRepository.setTextConfig(KEY_LABEL_TEMPLATE_QUANT, template)
    }

    fun getLabelTemplateKg(): String? = _labelTemplateKg.value.takeIf { it.isNotEmpty() }

    fun getLabelTemplateQuant(): String? = _labelTemplateQuant.value.takeIf { it.isNotEmpty() }

}














/*
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
//    private var automaticPrintingEnable = false

    private val _printerTypeLiveData = MutableStateFlow("")

    init {
        _printerTypeLiveData.update {
            configRepository.getTextConfig(ConfigRepository.PRINTER_TYPE) ?: "Network"
        }
    }

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
        _statusMessage.value = resourceProvider.getString(R.string.save_printer_success)
    }

    fun saveMgvConfig(password: String, store: String) {
        when {
            password.isEmpty() -> {
                _statusMessage.value = resourceProvider.getString(R.string.fill_keyword_field)
                return
            }

            password.length < 8 -> {
                _statusMessage.value = resourceProvider.getString(R.string.stronger_password_error)
                return
            }

            store.isEmpty() -> {
                _statusMessage.value = resourceProvider.getString(R.string.invalid_store_number)
                return
            }
        }

        val storeNumber = store.toIntOrNull()
        if (storeNumber == null) {
            _statusMessage.value = resourceProvider.getString(R.string.invalid_store_number)
            return
        }

        configRepository.setTextConfig(ConfigRepository.PASSWORD_MGV, password)
        configRepository.setIntConfig(ConfigRepository.STORE_NUMBER, storeNumber)
        _statusMessage.value = resourceProvider.getString(R.string.sucessfully_save_mgv_config)
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

//
//    fun saveServerAddress(address: String) {
//        if (address.isEmpty()) {
//            _statusMessage.value = resourceProvider.getString(R.string.invalid_address_server)
//            return
//        }
//
//        configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, address)
//        _statusMessage.value = resourceProvider.getString(R.string.sucessfully_save_server_address)
//    }

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

    /*
    fun enableAutomaticPrint(isEnable: Boolean) {
        automaticPrintingEnable = isEnable
        configRepository.setBooleanConfig(ConfigRepository.AUTOMATIC_PRINT, isEnable)

        _statusMessage.value = if (isEnable)
            resourceProvider.getString(R.string.automatic_print_enable)
        else
            resourceProvider.getString(R.string.automatic_print_disable)
    }
    */

    fun savePrinterUsbName(model: String, deviceName: String) {
        configRepository.setUsbConfig(model, deviceName)
    }

    fun savePrinterConfig(printerType: PrinterType) {
        configRepository.setTextConfig(ConfigRepository.PRINTER_TYPE, printerType.toString())

        _statusMessage.value = resourceProvider.getString(R.string.sucessfully_save_printer_type)
    }

    fun clearStatusMessage() {
        _statusMessage.value = ""
    }
}

*/






//FUNCIONA PORÉM NÃO PEGA A IMPRESSÃO AUTOMATICA
/*
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

//    fun savePrinterUsbName(printerName: String) {
//        configRepository.setUsbConfig(ConfigRepository.USB_PRINTER_NAME, printerName)
//    }

    fun savePrinterUsbName(printerName: String) {
        configRepository.setUsbConfig(printerName)
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

*/