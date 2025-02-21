package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.work.ExistingWorkPolicy
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.PrintWorker
import br.com.backupautomacao.centraldepesagem.PrintWorker.Companion.USB_PRINTER_NAME
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Label
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.security.InvalidParameterException

class MassProductionViewModel(application: Application) : AndroidViewModel(application) {
    private val _mgvProduct = MutableLiveData<MgvProduct?>()
    val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

    private val configRepository by lazy {
        getApplication<AppApplication>().configRepository
    }
    private val workManager by lazy {
        getApplication<AppApplication>().workManager
    }

    private val retrofitInstance by lazy {
        val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
        MgvApi.retrofitService(serverAddress)
    }

    fun fetchItem(code: String) {
        val mgvPassword = configRepository.getConfig(ConfigRepository.PASSWORD_MGV)!!
        val storeNumber = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString()

        if (code.isEmpty()) {
            Log.i("MassProductionViewModel", "fetchItem: Por favor digite o código do produto")
            return
        }

        viewModelScope.launch {
            try {
                val (mgvError, item) = retrofitInstance.queryItem(
                    item = code,
                    palavraChave = mgvPassword,
                    numeroLoja = storeNumber
                )

                mgvError?.let { error ->
                    Log.i("MainFragmentViewModel", "fetchItem: $error")
                    return@launch
                }

                Log.i("MassProductionViewModel", "fetchItem: $item")
                _mgvProduct.value = item

            } catch (e: IllegalArgumentException) {
                Log.i("MassProductionViewModel", "fetchItem: Endereço do servidor inválido")
            } catch (e: Exception) {
                Log.i(
                    "MassProductionViewModel",
                    "fetchItem: Erro ao fazer requisição - ${e.message}"
                )
            }
        }
    }

    fun updateWeight(weight: Double) {
        _mgvProduct.value?.weight = weight
    }

    fun print(quantity: String) {
        try {
            val product: MgvProduct = mgvProduct.value ?: MgvProduct()
            val label = Label(getApplication())
            val newLabel =
                label.generateLabelWithQuantity(product, "etiqueta_base.prn", quantity.toInt())

            if (configRepository.isUsbPrinterSelected()) {
                Log.i("MassProductionViewModel", "Impressora USB selecionada")
            } else {
                val printerIp: String? = configRepository.getConfig(ConfigRepository.PRINTER_IP)
                val printerPort: Int = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
                val usbPrinterName: String? = configRepository.getConfig(USB_PRINTER_NAME)
                val printTask = PrintWorker.createUniquePrintTask(
                    printerAddress = printerIp,
                    printerPort = printerPort,
                    label = newLabel,
                    usbPrinterName = usbPrinterName
                )
                workManager.enqueueUniqueWork(
                    PrintWorker.PRINT_TASK,
                    ExistingWorkPolicy.REPLACE,
                    printTask
                )
            }

            _mgvProduct.value = MgvProduct()
        } catch (e: InvalidParameterException) {
            Log.e("MassProductionViewModel", "Porta ou endereço IP inválidos: ${e.message}")
        } catch (e: FileNotFoundException) {
            Log.e("MassProductionViewModel", "Arquivo não encontrado: ${e.message}")
        } catch (e: Exception) {
            Log.e("MassProductionViewModel", "Erro inesperado ao imprimir: ${e.message}", e)
        }
    }
}