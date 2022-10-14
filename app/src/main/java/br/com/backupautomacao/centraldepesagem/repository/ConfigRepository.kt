package br.com.backupautomacao.centraldepesagem.repository

import android.content.Context
import android.content.SharedPreferences

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
  }

  fun setTextConfig(key: String, value: String) {
    editor.putString(key, value)
    editor.commit()
  }

  fun setBooleanConfig(key: String, value: Boolean) {
    editor.putBoolean(key, value)
    editor.commit()
  }

  fun getBooleanConfig(key: String): Boolean {
    return preferencias.getBoolean(key, false)
  }

  fun getConfig(key: String): String? {
    return preferencias.getString(key, "")
  }
}