package br.com.backupautomacao.centraldepesagem.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val PALAVRA_CHAVE = "bkp@2018"
private const val NUMERO_LOJA = "1"

class MgvRepository : KoinComponent {
  private val client: HttpClient by inject()

  suspend fun queryItem(
    keyWord: String = PALAVRA_CHAVE,
    storeNumber: String = NUMERO_LOJA,
    itemCode: String
  ): MgvProduct? {

    try {
      val response = client.get("ConsultaItem") {
        parameter("palavraChave", keyWord)
        parameter("codItem", itemCode)
        parameter("loja", storeNumber)
      }

      if (response.status.value in 200..209) {
        val (mgvError, item) = response.body<MgvResponseProduct>()
        mgvError?.let { error ->
          Log.i("MgvRepository", "queryItem: $error")
          when (error.code) {
            LOJA_NAO_ENCONTRADA -> throw MgvStoreNotFoundedException("Loja não encontrada")
            PRODUTO_NAO_ENCONTRADO -> throw MgvProductNotFoundedException("Produto não encontrado")
            else -> {
              Log.i("MgvRepository", "queryItem: ${error.code}")
              throw Exception("Erro inesperado")
            }
          }
        }

        return item?.let {
          Log.i("MgvRepository", "queryItem Reposta: $it")
          return@let it
        }
      }
      return null
    } catch (e: MgvStoreNotFoundedException) {
      throw e
    } catch (e: MgvProductNotFoundedException) {
      throw e
    }
  }

  suspend fun getNutriction(
    keyWord: String,
    storeNumber: String,
    codItem: Int
  ): MgvInformacoesNutricionais? {

    val response = client.get("ExportaInformacaoNutricional") {
      parameter("palavraChave", keyWord)
      parameter("loja", storeNumber)
    }

    if (response.status.value in 200..209) {
      val responseNutrictionFact: MgvResponseNutritionalInfo = response.body()
      return responseNutrictionFact.nutrictionFacts?.find { it.Codigo == codItem.toString() }
    }

    return null

  }

  suspend fun getExtraInfo(
    keyWord: String,
    storeNumber: String,
    codItem: Int
  ): ExtraInfo? {

    val response = client.get("ExportaInformacaoExtra") {
      parameter("palavraChave", keyWord)
      parameter("loja", storeNumber)
    }
    if (response.status.value in 200..209) {
      val mgvResponseExtraInfo: MgvResponseExtraInfo = response.body()
      return mgvResponseExtraInfo.extraInfos?.find { it.code == codItem.toString() }
    }
    return null
  }


}