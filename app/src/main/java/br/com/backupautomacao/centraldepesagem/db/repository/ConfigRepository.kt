package br.com.backupautomacao.centraldepesagem.db.repository

import android.content.Context
import android.content.SharedPreferences
import br.com.backupautomacao.centraldepesagem.utils.PrinterType

private const val NOME_ARQUIVO = "configuracoes"

class ConfigRepository(context: Context) {
  private val preferencias: SharedPreferences by lazy {
    context.getSharedPreferences(NOME_ARQUIVO, Context.MODE_PRIVATE)
  }
  private val editor: SharedPreferences.Editor by lazy {
    preferencias.edit()
  }

  companion object {
    const val PRINTER_IP = "ip"
    const val AUTOMATIC_PRINT = "automatic_print"
    const val SERVER_ADDRESS = "server"
    const val PRINTER_PORT = "printer_port"
    const val PASSWORD_MGV = "password_mgv"
    const val STORE_NUMBER = "store_number"
    const val PRINTER_TYPE = "printer_type"
    const val USB_PRINTER_NAME = "usb_printer_name"
  }

  fun setTextConfig(key: String, value: String) {
    editor.putString(key, value).apply()
  }

  fun getTextConfig(key: String): String? {
    return preferencias.getString(key, null)
  }

  fun setBooleanConfig(key: String, value: Boolean) {
    editor.putBoolean(key, value).apply()
  }

  fun getBooleanConfig(key: String): Boolean {
    return preferencias.getBoolean(key, false)
  }

  fun getConfig(key: String): String? {
    return preferencias.getString(key, "")
  }

  fun setIntConfig(key: String, value: Int) {
    editor.putInt(key, value).apply()
  }

  fun getIntConfig(key: String): Int {
    return preferencias.getInt(key, 0)
  }

  fun setPrinterType(printerType: String) {
    editor.putString(PRINTER_TYPE, printerType).apply()
  }

  fun getPrinterType(): PrinterType {
    val printerType = preferencias.getString(PRINTER_TYPE, "USB") ?: "USB"
    return when (printerType) {
      "USB" -> PrinterType.USB
      "Network" -> PrinterType.NETWORK
      else -> throw Exception("Nenhuma impressora selecionada")
    }
  }

  fun getUsbPrinterName(): String? {
    return preferencias.getString(USB_PRINTER_NAME, null)
  }

  fun setUsbPrinterName(key: String, value: String) {
    editor.putString(key, value).apply()
  }

  fun getPrinterIp(): String? {
    return preferencias.getString(PRINTER_IP, null)
  }

  fun getPrinterPort(): Int {
    return preferencias.getInt(PRINTER_PORT, 9100)
  }
}