package br.com.backupautomacao.centraldepesagem.repository

import android.content.Context
import android.content.SharedPreferences

class ConfigRepository(context: Context) {
  private val NOME_ARQUIVO = "configuracoes"
  private val preferencias: SharedPreferences by lazy {
    context.getSharedPreferences(NOME_ARQUIVO, Context.MODE_PRIVATE)
  }
  private val editor: SharedPreferences.Editor by lazy {
    preferencias.edit()
  }

  companion object {
    const val PRINTER_IP = "ip"
    const val MODELO_BALANCA = "balanca"
    const val COMUNICACAO = "comunicacao"
  }

  fun setConfiguracao(key: String, ip: String) {
    editor.putString(key, ip)
    editor.commit()
  }

  fun getConfig(key: String): String? {
    return preferencias.getString(key, "")
  }
}