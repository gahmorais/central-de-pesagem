package br.com.backupautomacao.centraldepesagem.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.Exception

private const val BASE_URL = "http://172.16.0.14:9300/MGV6_WCF/REST/"
private const val PALAVRA_CHAVE = "bkp@2018"
private const val NUMERO_LOJA = "1"

//private val retrofit by lazy {
//  Retrofit.Builder()
//    .addConverterFactory(GsonConverterFactory.create())
//    .baseUrl(BASE_URL)
//    .build()
//}

private fun retrofitFactory(baseUrl: String): Retrofit {
  return Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(baseUrl)
    .build()
}

interface MgvService {
  @GET("ConsultaItem")
  suspend fun queryItem(
    @Query("palavraChave") palavraChave: String = PALAVRA_CHAVE,
    @Query("loja") numeroLoja: String = NUMERO_LOJA,
    @Query("codItem") item: String
  ): MgvResponseProduct

  @GET("ExportaInformacaoNutricional")
  suspend fun queryNutritionalInfo(
    @Query("palavraChave") palavraChave: String = PALAVRA_CHAVE,
    @Query("loja") numeroLoja: String = NUMERO_LOJA
  ): MgvResponseNutritionalInfo

  @GET("ExportaInformacaoExtra")
  suspend fun queryExtraInfo(
    @Query("palavraChave") palavraChave: String = PALAVRA_CHAVE,
    @Query("loja") numeroLoja: String = NUMERO_LOJA
  ): MgvResponseExtraInfo
}

object MgvApi {
  fun retrofitService(baseUrl: String?): MgvService {
    return baseUrl?.let { retrofitFactory(baseUrl).create(MgvService::class.java) }
      ?: throw Exception("Endereço do servidor inválido")
  }
}
