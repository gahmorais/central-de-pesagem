package br.com.backupautomacao.pesagem_modular.network

import br.com.backupautomacao.pesagem_modular.Constants.ERRO_LOJA_NAO_ENCONTRADA
import br.com.backupautomacao.pesagem_modular.Constants.ERRO_PRODUTO_NAO_ENCONTRADO
import br.com.backupautomacao.pesagem_modular.exceptions.MgvProductNotFoundedException
import br.com.backupautomacao.pesagem_modular.exceptions.MgvStoreNotFoundedException
import br.com.backupautomacao.pesagem_modular.models.MgvExtraInfo
import br.com.backupautomacao.pesagem_modular.models.MgvNutritionalInfo
import br.com.backupautomacao.pesagem_modular.models.MgvProductImpl
import br.com.backupautomacao.pesagem_modular.models.MgvResponseExtraInfo
import br.com.backupautomacao.pesagem_modular.models.MgvResponseNutritionalInfo
import br.com.backupautomacao.pesagem_modular.models.MgvResponseProduct
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiMgv(
  private val client: HttpClient
) : IApiMgv {

  override suspend fun queryItem(
    keyWord: String,
    storeNumber: String,
    itemCode: String
  ): MgvProductImpl {
    val response = client.get("ConsultaItem") {
      parameter("palavraChave", keyWord)
      parameter("codItem", itemCode)
      parameter("loja", storeNumber)
    }

    if (response.status.value in 200..209) {
      val mgvResponse = response.body<MgvResponseProduct>()
      if (mgvResponse.mgvError != null) {
        val error = mgvResponse.mgvError
        when (error.code) {
          ERRO_LOJA_NAO_ENCONTRADA -> throw MgvStoreNotFoundedException("Loja não encontrada")
          ERRO_PRODUTO_NAO_ENCONTRADO -> throw MgvProductNotFoundedException("Produto não encontrado")
          else -> throw Exception("Erro inesperado ${error.message}")
        }
      }

      val item = mgvResponse.item ?: throw Exception("Produto nulo")

      if (item.codNutrictionFacts != null) {
        val code = item.codNutrictionFacts
        item.nutrictionFacts = withContext(Dispatchers.IO) {
          getNutrictionByCode(keyWord, storeNumber, code.toString())
        }
      }

      if (item.codExtraInfo != null) {
        val code = item.codExtraInfo
        item.extraInfo = withContext(Dispatchers.IO) {
          getExtraInfo(keyWord, storeNumber, code)
        }
      }

      return mgvResponse.item
    }

    throw Exception("Erro ao fazer requisição status code: ${response.status.value}")
  }

  override suspend fun getNutriction(
    keyWord: String,
    storeNumber: String,
  ): List<MgvNutritionalInfo> {

    val response = client.get("ExportaInformacaoNutricional") {
      parameter("palavraChave", keyWord)
      parameter("loja", storeNumber)
    }

    if (response.status.value in 200..209) {
      val contentNutrictionFact: MgvResponseNutritionalInfo = response.body()
      if (contentNutrictionFact.mgvError != null) {
        val error = contentNutrictionFact.mgvError
        throw Exception("Erro na requisição ${error.message}")
      }
      return contentNutrictionFact.nutrictionFacts
        ?: throw Exception("Não há dados de informação nutricional")

    }

    throw Exception("Erro na requisição info. nutricional status code ${response.status.value}")

  }

  override suspend fun getNutrictionByCode(
    keyWord: String,
    storeNumber: String,
    code: String
  ): MgvNutritionalInfo {
    val nutrictionList = try {
      getNutriction(keyWord, storeNumber)
    } catch (e: Exception) {
      throw e
    }

    val nutrictionResponse = nutrictionList.find { it.Codigo == code }
    if (nutrictionResponse != null) {
      return nutrictionResponse
    }

    throw Exception("Informação nutricional não encontrada")
  }

  override suspend fun getExtraInfo(
    keyWord: String,
    storeNumber: String,
    codItem: Int
  ): MgvExtraInfo {

    val response = client.get("ExportaInformacaoExtra") {
      parameter("palavraChave", keyWord)
      parameter("loja", storeNumber)
    }
    if (response.status.value in 200..209) {
      val mgvResponseExtraInfo: MgvResponseExtraInfo = response.body()
      if (mgvResponseExtraInfo.mgvError != null) {
        val error = mgvResponseExtraInfo.mgvError
        throw Exception("Erro na requisição ${error.message}")
      }
      return mgvResponseExtraInfo.mgvExtraInfos?.find { it.code == codItem.toString() }
        ?: throw Exception("Não há dados de informação extra")
    }
    throw Exception("Erro na requisição ${response.status.value}")
  }


}