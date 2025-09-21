package br.com.backupautomacao.pesagem_modular.workers

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import br.com.backupautomacao.pesagem_modular.R
import timber.log.Timber
import java.io.PrintWriter
import java.net.Socket

class NetworkWorker(appContext: Context, workerParameters: WorkerParameters) :
    Worker(appContext, workerParameters) {

    companion object {
        const val PRINTER_ADDRESS = "printer_address"
        const val PRINTER_PORT = "printer_port"
        fun createUniquePrintTask(
            printerAddress: String?,
            printerPort: Int,
            label: String
        ): OneTimeWorkRequest {

            val data: Data = Data.Builder()
                .putString(PRINT_LABEL, label)
                .putString(PRINTER_ADDRESS, printerAddress)
                .putInt(PRINTER_PORT, printerPort)
                .build()

            return OneTimeWorkRequestBuilder<NetworkWorker>()
                .setInputData(data)
                .build()
        }
    }

    override fun doWork(): Result {
        val label = inputData.getString(PRINT_LABEL) ?: return Result.failure(
            createDataMessage(
                applicationContext.getString(R.string.printing_error)
            )
        )

        val printerAddress = inputData.getString(PRINTER_ADDRESS) ?: return Result.failure(
            createDataMessage("Impressora com ip inválido")
        )
        val printerPort = inputData.getInt(PRINTER_PORT, 9100)

        return try {
            Socket(printerAddress, printerPort).use { socket ->
                PrintWriter(socket.getOutputStream()).use { pw ->
                    pw.write(label)
                    pw.flush()
                }
            }
            Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
        } catch (e: Exception) {
            Timber.Forest.e(e, "Erro ao imprimir via rede")
            Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
        }
    }

    override fun onStopped() {
        super.onStopped()
        Timber.Forest.i("onStopped: Trabalho encerrado")
    }
}