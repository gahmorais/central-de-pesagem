package br.com.backupautomacao.tabletinteligente.utils

import android.widget.TextView

class Validate {
  companion object {
    fun validaCampoDeTexto(campoDeTexto: TextView): String {
      if (campoDeTexto.text != null) {
        return campoDeTexto.text.toString()
      }
      return ""
    }

    fun validaIp(ip: String): Boolean {
      return ip.matches("(\\d\\d\\d\\.){3}\\d\\d\\d".toRegex())
    }
  }
}

