package br.com.backupautomacao.centraldepesagem

class Constants private constructor() {
  companion object {
    const val INTENT_ACTION_GRANT_USB = "com.android.example.USB_PERMISSION"
    const val INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect"
    const val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel"
    const val INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity"
    const val PERMISSION_BLUETOOTH_REQUEST_CODE = 201
    const val PERMISSION_STORAGE_REQUEST_CODE = 200
    const val REQUEST_ENABLE_BT = 1
    const val MESSAGE_READ = 2
    const val CONNECTING_STATUS = 3
    const val CONNECTED_STATUS = 4
    const val ERROR_MESSAGE = 5
    const val MESSAGE_CONNECTING = "Conectando..."
    const val MESSAGE_PRINTING_ERROR = "Não foi possível realizar impressão"
    const val MESSAGE_ERROR_IP_NOT_SETTED = "IP do equipamento não configurado"
    val ERROR_GET_PRODUCT = { text: String -> "Busque o produto antes de fazer a $text" }
    const val ERROR_MAKE_WEIGHING = "Realize a pesagem"
    const val ERROR_REQUEST = "Erro ao fazer requisição"
    val ERROR_RESPONSE_MGV = { msg: String, code: String -> "${msg} Código: ${code}" }
    val MESSAGE_SCALE_CONNECTED = { number: Int? -> "Balança $number conectada" }
  }
}