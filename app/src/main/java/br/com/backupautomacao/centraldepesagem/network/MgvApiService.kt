package br.com.backupautomacao.centraldepesagem.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.Exception
import java.util.concurrent.TimeUnit

private const val PALAVRA_CHAVE = "bkp@2018"
private const val NUMERO_LOJA = "1"

private fun retrofitFactory(baseUrl: String): Retrofit {
    return Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient())
        .baseUrl(baseUrl)
        .build()
}

private fun createOkHttpClient() = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor { message ->
        println("LOG APP: $message")
    }.apply { level = HttpLoggingInterceptor.Level.BODY })
    .addNetworkInterceptor(HttpLoggingInterceptor { message ->
        println("LOG NTW: $message")
    }.apply { level = HttpLoggingInterceptor.Level.BODY })
    .build()

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
