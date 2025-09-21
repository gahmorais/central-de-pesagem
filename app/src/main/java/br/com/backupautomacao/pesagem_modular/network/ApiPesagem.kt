package br.com.backupautomacao.pesagem_modular.network

import com.google.gson.Gson
import timber.log.Timber

class ApiPesagem(
    private val apiService: MgvService,
    private val password: String,
    private val store: String
) {

    suspend fun getProduct(code: String): MgvProduct = try {
        val response = apiService.queryItem(password, store, code)
        if (response.code() in 400..599) {
            val errorBody = response.errorBody()?.string()
            val error = Gson().fromJson(errorBody, RequestResponse::class.java)
            throw Exception(error.mgvError?.message)
        }

        val responseBody = response.body()
        if (responseBody?.item == null) {
            throw Exception("O item é nulo")
        }
        responseBody.item
    } catch (e: Exception) {
        e.printStackTrace()
        throw Exception(e.message)
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "fetchItem: endereço da api inválido")
        throw e
    } catch (e: Exception) {
        Timber.e(e, "fetchItem: erro inesperado")
        throw e
    }
}