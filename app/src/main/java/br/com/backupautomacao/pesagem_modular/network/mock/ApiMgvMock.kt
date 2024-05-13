package br.com.backupautomacao.pesagem_modular.network.mock

import br.com.backupautomacao.pesagem_modular.exceptions.MgvProductNotFoundedException
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
    val product = getProductMock(itemCode)
    if (product != null) return product
    throw MgvProductNotFoundedException("Produto não encontrado")
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
    return MgvNutritionalInfo(
      Codigo = "2",
      Carboidrato = 0.6,
      DescricaoMedidaCaseira = 0,
      FibraAlimentar = null,
      FracaoMedidaCaseira = 0.0,
      GorduraSaturada = 3.9,
      GorduraTotal = 6.7,
      GorduraTrans = null,
      MedidaCaseira = 0.0,
      Observacao = "",
      Proteina = 6.6,
      QtdPorcao = 30.0,
      Sodio = 188.0,
      UnidadePorcao = 0.0,
      ValorEnergetico = 2000
    )
  }

  override suspend fun getExtraInfo(
    keyWord: String,
    storeNumber: String,
    codItem: Int
  ): MgvExtraInfo? {
    return null
  }
}