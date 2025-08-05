package br.com.backupautomacao.centraldepesagem.utils

enum class PrinterType {
  NETWORK,
  USB;

  companion object {
    fun from(value: String?): PrinterType {
      return when (value?.uppercase()) {
        "NETWORK" -> NETWORK
        else -> USB
      }
    }
  }
}