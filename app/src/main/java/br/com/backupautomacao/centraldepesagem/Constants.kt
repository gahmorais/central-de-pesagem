package br.com.backupautomacao.centraldepesagem

class Constants private constructor() {
  companion object {
    const val CODE_MESSAGE_READ = 2
    const val CODE_CONNECTING_STATUS = 3
    const val CODE_CONNECTED_STATUS = 4
    const val CODE_ERROR_MESSAGE = 5
    const val COMMUNICATTION_ERROR = "Falha na comunicação"
    const val ONE_DAY_MILLIS = 60 * 60 * 24 * 1000
  }
}