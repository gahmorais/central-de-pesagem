package br.com.backupautomacao.pesagem_modular.network

import br.com.backupautomacao.pesagem_modular.Constants.NUMERO_LOJA
import br.com.backupautomacao.pesagem_modular.Constants.PALAVRA_CHAVE
import br.com.backupautomacao.pesagem_modular.models.MgvExtraInfo
import br.com.backupautomacao.pesagem_modular.models.MgvNutritionalInfo
import br.com.backupautomacao.pesagem_modular.models.MgvProductImpl

interface IApiMgv {
  suspend fun queryItem(
    keyWord: String = PALAVRA_CHAVE,
    storeNumber: String = NUMERO_LOJA,
    itemCode: String
  ): MgvProductImpl

  suspend fun getNutriction(
    keyWord: String,
    storeNumber: String,
  ):  List<MgvNutritionalInfo>

  suspend fun getNutrictionByCode(
    keyWord: String,
    storeNumber: String,
    code: String
  ): MgvNutritionalInfo

  suspend fun getExtraInfo(
    keyWord: String,
    storeNumber: String,
    codItem: Int
  ): MgvExtraInfo?
}