package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.Gson
import timber.log.Timber
import java.net.HttpURLConnection

class ApiPesagem(
  private val apiService: MgvService,
  private val password: String,
  private val store: String
) {

  suspend fun getProduct(code: String): MgvProduct = try {
    val response = apiService.queryItem(password, store, code)
    if (response.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
      val requestResponse = Gson().fromJson(response.body().toString(), RequestResponse::class.java)
      Timber.i("Body: ${response.body().toString()}")
      throw Exception(requestResponse.mgvError?.message)
    }
    val requestResult = Gson().fromJson(response.body().toString(), RequestResponse::class.java)

    if (requestResult.mgvError != null) {
      throw Exception(requestResult.mgvError.message)//requestResult.mgvError.message)
    }

    if (requestResult.item == null) {
      throw Exception("Item nulo")
    }

    requestResult.item

  } catch (e: Exception) {
    e.printStackTrace()
    throw Exception("OCORREU UM EROO - ${e.message}")
  } catch (e: IllegalArgumentException) {
    Timber.e(e, "fetchItem: endereço da api inválido")
    throw e
  } catch (e: Exception) {
    Timber.e(e, "fetchItem: erro inesperado")
    throw e
  }
}