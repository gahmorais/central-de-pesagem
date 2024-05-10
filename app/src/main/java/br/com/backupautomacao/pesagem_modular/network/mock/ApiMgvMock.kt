package br.com.backupautomacao.pesagem_modular.network.mock

import br.com.backupautomacao.pesagem_modular.models.MgvExtraInfo
import br.com.backupautomacao.pesagem_modular.models.MgvNutritionalInfo
import br.com.backupautomacao.pesagem_modular.models.MgvProductImpl
import br.com.backupautomacao.pesagem_modular.network.IApiMgv

class ApiMgvMock : IApiMgv {
  override suspend fun queryItem(
    keyWord: String,
    storeNumber: String,
    itemCode: String
  ): MgvProductImpl {
    TODO("Not yet implemented")
  }

  override suspend fun getNutriction(
    keyWord: String,
    storeNumber: String
  ): List<MgvNutritionalInfo> {
    TODO("Not yet implemented")
  }

  override suspend fun getNutrictionByCode(
    keyWord: String,
    storeNumber: String,
    code: String
  ): MgvNutritionalInfo {
    TODO("Not yet implemented")
  }

  override suspend fun getExtraInfo(
    keyWord: String,
    storeNumber: String,
    codItem: Int
  ): MgvExtraInfo? {
    TODO("Not yet implemented")
  }
}