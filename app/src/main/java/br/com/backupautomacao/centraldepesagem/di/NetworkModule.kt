package br.com.backupautomacao.centraldepesagem.di

import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import org.koin.dsl.module

val networkModule = module {
  single {
    val configRepository = get<ConfigRepository>()
    HttpClient(CIO) {
      install(ContentNegotiation) {
        gson()
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 3000
      }
      install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.BODY
      }
      defaultRequest {
        val server = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
        server?.let {
          url(it)
        }
      }
    }
  }
}