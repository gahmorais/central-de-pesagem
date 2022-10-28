package br.com.backupautomacao.centraldepesagem

class Constants private constructor() {
  companion object {
    const val SUCCESS_TO_SAVE_MGV_CONFIG = "Configuração do MGV salva com sucesso!"
    const val PERMISSION_BLUETOOTH_REQUEST_CODE = 201
    const val PERMISSION_STORAGE_REQUEST_CODE = 200
    const val REQUEST_ENABLE_BT = 1
    const val CODE_MESSAGE_READ = 2
    const val CODE_CONNECTING_STATUS = 3
    const val CODE_CONNECTED_STATUS = 4
    const val CODE_ERROR_MESSAGE = 5
    const val MESSAGE_CONNECTING = "Conectando..."
    const val PRINTING_ERROR = "Não foi possível realizar impressão"
    const val IP_NOT_SET_ERROR = "IP do equipamento não configurado"
    const val MAKE_WEIGHING_ERROR = "Realize a pesagem"
    const val REQUEST_ERROR = "Erro ao fazer requisição"
    const val PRODUCT_NOT_FOUND_ERROR = "Produto não encontrado!"
    const val SET_SCALE_ERROR = "Não foi possíve definir a balança"
    const val PRINTER_NOT_FOUND_ERROR = "Impressora não encontrada"
    const val PORT_ERROR = "Porta inválida"
    const val SERVER_ADDRESS_ERROR = "Endereço do servidor inválido"
    const val LISTENING_MESSAGE = "Ouvindo..."
    const val COMMUNICATTION_ERROR = "Falha na comunicação"
    const val SUCESS_TO_SAVE_SERVER_ADDRESS = "Endereço do servidor salvo com sucesso"
    const val MESSAGE_AUTOMATIC_PRINT_ENABLE = "Impressão automática ativada"
    const val MESSAGE_AUTOMATIC_PRINT_DISABLE = "Impressão automática desativada"
    const val SUCCESS_TO_SAVE_PRINTER_ADDRESS = "Configuração da impressora salva com sucesso!"
    const val EMPTY_PASSWORD_ERROR = "Por favor, preencha o campo da palavra chave"
    const val ITEM_CODE_ERROR = "Por favor, digite o código do item"
    const val STRONGER_PASSWORD_ERROR = "Por favor, utilize uma senha mais forte"
    const val INVALID_STORE_NUMBER_ERROR = "O número da loja inválido"
    fun errorGetProdut(text: String) = "Busque o produto antes de fazer a $text"
    fun errorResponseMgv(msg: String, code: String) = "$msg Código: $code"
    fun messageScaleConnected(number: Int?) = "Balança $number conectada"
    fun messageClientConnected(ip: String) = "Cliente conectado do IP: $ip"
  }
}