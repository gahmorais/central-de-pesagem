package br.com.backupautomacao.centraldepesagem

class Constants private constructor() {
  companion object {

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
    const val ERROR_MAKE_WEIGHING = "Realize a pesagem"
    const val ERROR_REQUEST = "Erro ao fazer requisição"
    const val ERROR_PRODUCT_NOT_FOUND = "Produto não encontrado!"
    const val ERROR_TO_SET_SCALE = "Não foi possíve definir a balança"
    const val ERROR_PRINTER_NOT_FOUND = "Impressora não encontrada"
    val ERROR_GET_PRODUCT = { text: String -> "Busque o produto antes de fazer a $text" }
    val ERROR_RESPONSE_MGV = { msg: String, code: String -> "$msg Código: $code" }
    val MESSAGE_SCALE_CONNECTED = { number: Int? -> "Balança $number conectada" }
  }
}