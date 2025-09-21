package br.com.backupautomacao.pesagem_modular.utils

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