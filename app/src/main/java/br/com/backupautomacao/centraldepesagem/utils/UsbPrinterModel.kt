package br.com.backupautomacao.centraldepesagem.utils

enum class UsbPrinterModel {
    EPSON,
    HONEYWELL;

    companion object {
        fun fromString(value: String?): UsbPrinterModel {
            return try {
                value?.let { value.uppercase() }?.let { UsbPrinterModel.valueOf(it) }
                    ?: HONEYWELL
            } catch (e: Exception) {
                HONEYWELL
            }
        }
    }
}
