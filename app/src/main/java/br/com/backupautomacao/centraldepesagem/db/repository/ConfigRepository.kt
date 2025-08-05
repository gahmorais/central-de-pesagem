package br.com.backupautomacao.centraldepesagem.db.repository

import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import android.content.Context
import android.content.SharedPreferences
import br.com.backupautomacao.centraldepesagem.utils.UsbPrinterModel

private const val NOME_ARQUIVO = "configuracoes"

class ConfigRepository(context: Context) {
  private val preferencias: SharedPreferences by lazy {
    context.getSharedPreferences(NOME_ARQUIVO, Context.MODE_PRIVATE)
  }

  companion object {
    const val PRINTER_IP = "ip"
    const val AUTOMATIC_PRINT = "automatic_printing"
    const val SERVER_ADDRESS = "server"
    const val PRINTER_PORT = "printer_port"
    const val PASSWORD_MGV = "password_mgv"
    const val STORE_NUMBER = "store_number"
    const val PRINTER_TYPE = "printer_type"
    const val USB_PRINTER_NAME = "usb_printer_name"
    const val LABEL_TEMPLATE_NAME = "LABEL_TEMPLATE_NAME"
    private const val KEY_LABEL_TEMPLATE_KG = "LABEL_TEMPLATE_KG"
    private const val KEY_LABEL_TEMPLATE_QUANT = "LABEL_TEMPLATE_QUANT"

  }

  fun setTextConfig(key: String, value: String) {
    preferencias.edit().putString(key, value).apply()
  }

  fun getTextConfig(key: String): String? {
    return preferencias.getString(key, null)
  }

  fun getConfig(key: String): String? {
    return preferencias.getString(key, "")
  }

  fun setBooleanConfig(key: String, value: Boolean) {
    preferencias.edit().putBoolean(key, value).apply()
  }

  fun getBooleanConfig(key: String): Boolean {
    return preferencias.getBoolean(key, false)
  }

  fun setIntConfig(key: String, value: Int) {
    preferencias.edit().putInt(key, value).apply()
  }

  fun getIntConfig(key: String): Int {
    return preferencias.getInt(key, 0)
  }

  fun setPrinterType(printerType: String) {
    preferencias.edit().putString(PRINTER_TYPE, printerType).apply()
  }

  fun getPrinterType(): PrinterType {
    val printerType = preferencias.getString(PRINTER_TYPE, "USB") ?: "USB"
    return when (printerType.uppercase()) {
      "USB" -> PrinterType.USB
      "NETWORK" -> PrinterType.NETWORK
      else -> throw Exception("Nenhuma impressora selecionada")
    }
  }

  fun setUsbConfig(model: String, deviceName: String) {
    val fullName = "$model;$deviceName"
    preferencias.edit().putString(USB_PRINTER_NAME, fullName).apply()
  }

  fun getUsbConfig(): String? {
    return preferencias.getString(USB_PRINTER_NAME, null)
  }

  fun getUsbPrinterModel(): UsbPrinterModel {
    val fullConfig = getUsbConfig() ?: return UsbPrinterModel.HONEYWELL
    val modelName = fullConfig.split(";").firstOrNull()
    return UsbPrinterModel.fromString(modelName)
  }

  fun getPrinterIp(): String? {
    return preferencias.getString(PRINTER_IP, null)
  }

  fun getPrinterPort(): Int {
    return preferencias.getInt(PRINTER_PORT, 9100)
  }

  fun initializeDefaults() {
    if (!containsKey(AUTOMATIC_PRINT)) {
      setBooleanConfig(AUTOMATIC_PRINT, false)
    }

    if (!containsKey(PRINTER_TYPE)) {
      setPrinterType(PrinterType.NETWORK.toString())
    }

    if (!containsKey(STORE_NUMBER)) {
      setIntConfig(STORE_NUMBER, 1)
    }

    if (!containsKey(PRINTER_PORT)) {
      setIntConfig(PRINTER_PORT, 9100)
    }

    if (!containsKey(SERVER_ADDRESS)) {
      setTextConfig(SERVER_ADDRESS, "")
    }
  }

  private fun containsKey(key: String): Boolean {
    return preferencias.contains(key)
  }
}





/*
import android.content.Context
import android.content.SharedPreferences
import br.com.backupautomacao.centraldepesagem.utils.UsbPrinterModel
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
    const val AUTOMATIC_PRINT = "automatic_printing"
    const val SERVER_ADDRESS = "server"
    const val PRINTER_PORT = "printer_port"
    const val PASSWORD_MGV = "password_mgv"
    const val STORE_NUMBER = "store_number"
    const val PRINTER_TYPE = "printer_type"
    const val USB_PRINTER_NAME = "usb_printer_name"
    const val LABEL_TEMPLATE_NAME = "LABEL_TEMPLATE_NAME"
    private const val KEY_LABEL_TEMPLATE_KG = "LABEL_TEMPLATE_KG"
    private const val KEY_LABEL_TEMPLATE_QUANT = "LABEL_TEMPLATE_QUANT"
  }

  fun setTextConfig(key: String, value: String) {
    editor.putString(key, value).apply()
  }

  fun getTextConfig(key: String): String? {
    return preferencias.getString(key, null)
  }

  fun getConfig(key: String): String? {
    return preferencias.getString(key, "")
  }

  fun setBooleanConfig(key: String, value: Boolean) {
    editor.putBoolean(key, value).apply()
  }

  fun getBooleanConfig(key: String): Boolean {
    return preferencias.getBoolean(key, false)
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
    return when (printerType.uppercase()) {
      "USB" -> PrinterType.USB
      "NETWORK" -> PrinterType.NETWORK
      else -> throw Exception("Nenhuma impressora selecionada")
    }
  }

  fun getUsbConfig(): String? {
    return preferencias.getString(USB_PRINTER_NAME, null)
  }

  fun setUsbConfig(model: String, deviceName: String) {
    val fullName = "$model;$deviceName"
    editor.putString(USB_PRINTER_NAME, fullName).apply()
  }


  fun getPrinterIp(): String? {
    return preferencias.getString(PRINTER_IP, null)
  }

  fun getUsbPrinterModel(): UsbPrinterModel {
    val fullConfig = getUsbConfig() ?: return UsbPrinterModel.HONEYWELL
    val modelName = fullConfig.split(";").firstOrNull()
    return UsbPrinterModel.fromString(modelName)
  }


  fun getPrinterPort(): Int {
    return preferencias.getInt(PRINTER_PORT, 9100)
  }

  fun initializeDefaults() {
    if (!containsKey(AUTOMATIC_PRINT)) {
      setBooleanConfig(AUTOMATIC_PRINT, false)
    }

    if (!containsKey(PRINTER_TYPE)) {
      setPrinterType(PrinterType.NETWORK.toString())
    }

    if (!containsKey(STORE_NUMBER)) {
      setIntConfig(STORE_NUMBER, 1)
    }

    if (!containsKey(PRINTER_PORT)) {
      setIntConfig(PRINTER_PORT, 9100)
    }

    if (!containsKey(SERVER_ADDRESS)) {
      setTextConfig(SERVER_ADDRESS, "")
    }
  }

  private fun containsKey(key: String): Boolean {
    return preferencias.contains(key)
  }
}

 */







/*
import android.content.Context
import android.content.SharedPreferences
import br.com.backupautomacao.centraldepesagem.utils.UsbPrinterModel
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

  fun setIntConfig(key: String, value: Int) {
    editor.putInt(key, value).apply()
  }

  fun getIntConfig(key: String): Int {
    return preferencias.getInt(key, 0)
  }

  fun getConfig(key: String): String? {
    return preferencias.getString(key, "")
  }

  fun setPrinterType(printerType: String) {
    editor.putString(PRINTER_TYPE, printerType).apply()
  }

  fun getPrinterType(): PrinterType {
    val printerType = preferencias.getString(PRINTER_TYPE, "USB") ?: "USB"
    return when (printerType) {
      "USB" -> PrinterType.USB
      "NETWORK" -> PrinterType.NETWORK
      else -> throw Exception("Nenhuma impressora selecionada")
    }
  }

  fun getUsbConfig(): String? {
    return preferencias.getString(USB_PRINTER_NAME, null)
  }

  fun setUsbConfig(deviceName: String) {
    editor.putString(USB_PRINTER_NAME, deviceName).apply()
  }

  fun getPrinterIp(): String? {
    return preferencias.getString(PRINTER_IP, null)
  }

  fun getUsbPrinterModel(): UsbPrinterModel {
    return UsbPrinterModel.HONEYWELL
  }

  fun getPrinterPort(): Int {
    return preferencias.getInt(PRINTER_PORT, 9100)
  }

  fun initializeDefaults() {
    if (!containsKey(AUTOMATIC_PRINT)) {
      setBooleanConfig(AUTOMATIC_PRINT, false)
    }

    if (!containsKey(PRINTER_TYPE)) {
      setPrinterType(PrinterType.NETWORK.toString())
    }

    if (!containsKey(STORE_NUMBER)) {
      setIntConfig(STORE_NUMBER, 1)
    }

    if (!containsKey(PRINTER_PORT)) {
      setIntConfig(PRINTER_PORT, 9100)
    }

    if (!containsKey(SERVER_ADDRESS)) {
      setTextConfig(SERVER_ADDRESS, "")
    }
  }

  private fun containsKey(key: String): Boolean {
    return preferencias.contains(key)
  }

}

*/