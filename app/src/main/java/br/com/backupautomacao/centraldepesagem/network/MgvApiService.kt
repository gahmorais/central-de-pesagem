package br.com.backupautomacao.centraldepesagem.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "http://172.16.0.14:9300/MGV6_WCF/REST/"
private const val PALAVRA_CHAVE = "bkp@2018"
private const val NUMERO_LOJA = "1"

private val retrofit by lazy {
  Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
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
}

object MgvApi {
  val retrofitService: MgvService by lazy {
    retrofit.create(MgvService::class.java)
  }
}
