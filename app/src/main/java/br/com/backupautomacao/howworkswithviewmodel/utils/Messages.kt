package br.com.backupautomacao.tabletinteligente.utils

import android.content.Context
import android.widget.Toast

class Messages {
  companion object {
    fun toast(context: Context, mensagem: String) {
      Toast.makeText(context, mensagem, Toast.LENGTH_LONG).show()
    }
  }
}