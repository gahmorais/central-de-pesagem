package br.com.backupautomacao.pesagem_modular.utils

class Validate {
  companion object {
    fun validaIp(ip: String): Boolean {
      return ip.matches("(\\d\\d\\d\\.){3}\\d\\d\\d".toRegex())
    }

    fun validaPorta(port: String): Boolean {
      return port.isNotEmpty() && port.length > 2
    }

  }
}

