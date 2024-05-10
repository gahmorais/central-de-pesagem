package br.com.backupautomacao.pesagem_modular.data.repository

import br.com.backupautomacao.pesagem_modular.Constants
import br.com.backupautomacao.pesagem_modular.data.AppDatabase
import br.com.backupautomacao.pesagem_modular.data.model.Config
import br.com.backupautomacao.pesagem_modular.models.Printer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val NOME_ARQUIVO = "configuracoes"

class ConfigRepository(private val database: AppDatabase) {
  suspend fun getPrinterConfig(): Printer = try {
    val configAddress = withContext(Dispatchers.IO) {
      database.configDao().getConfig(Constants.PRINTER_IP)
    }
    val configPort = withContext(Dispatchers.IO) {
      database.configDao().getConfig(Constants.PRINTER_PORT)
    }

    val address = configAddress?.value ?: ""
    val port = configPort?.value?.toInt() ?: 9100
    Printer(address, port)
  } catch (e: Exception) {
    throw e
  }

  suspend fun getServerPassword(): String = try {
    val configPassword = withContext(Dispatchers.IO) {
      database.configDao().getConfig(Constants.PASSWORD_MGV)?.value ?: ""
    }
    configPassword
  } catch (e: Exception) {
    throw e
  }

  suspend fun getStoreNumber(): String = try {
    val configStoreNumber = withContext(Dispatchers.IO) {
      database.configDao().getConfig(Constants.STORE_NUMBER)?.value ?: ""
    }
    configStoreNumber
  } catch (e: Exception) {
    throw e
  }

  fun getServerAddress(): String = runBlocking {
    try {
      val configServerAddress = withContext(Dispatchers.IO) {
        database.configDao().getConfig(Constants.SERVER_ADDRESS)
      }
      configServerAddress?.value ?: ""
    } catch (e: Exception) {
      throw e
    }
  }

  suspend fun getAutomaticPrint(): Boolean = try {
    val configAutomaticPrint = withContext(Dispatchers.IO) {
      database.configDao().getConfig(Constants.AUTOMATIC_PRINT)
    }
    configAutomaticPrint?.value == "1"
  } catch (e: Exception) {
    throw e
  }

  suspend fun setPrinterConfig(address: String, port: Int) = try {
    val addressConfig = Config(Constants.PRINTER_IP, address)
    val portConfig = Config(Constants.PRINTER_PORT, port.toString())
    withContext(Dispatchers.IO) {
      database.configDao().setConfig(addressConfig)
    }
    withContext(Dispatchers.IO) {
      database.configDao().setConfig(portConfig)
    }
  } catch (e: Exception) {
    throw e
  }

  suspend fun setServerAddress(address: String) = try {
    val serverConfig = Config(Constants.SERVER_ADDRESS, address)
    database.configDao().setConfig(serverConfig)
  } catch (e: Exception) {
    throw e
  }

  suspend fun setStoreNumber(number: Int) = try {
    val configStore = Config(Constants.STORE_NUMBER, number.toString())
    database.configDao().setConfig(configStore)
  } catch (e: Exception) {
    throw e
  }

  suspend fun setServerPassword(password: String) = try {
    val configPassword = Config(Constants.PASSWORD_MGV, password)
    database.configDao().setConfig(configPassword)
  } catch (e: Exception) {
    throw e
  }

  suspend fun setAutomaticPrint(isEnabled: Boolean) = try {
    val configAutomaticPrint = Config(Constants.AUTOMATIC_PRINT, if (isEnabled) "1" else "0")
    database.configDao().setConfig(configAutomaticPrint)
  } catch (e: Exception) {
    throw e
  }
}