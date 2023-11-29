package br.com.backupautomacao.pesagem_modular

import android.content.Context
import android.util.Log
import androidx.work.*
import java.io.PrintWriter
import java.net.Socket
import java.security.InvalidParameterException

class PrintWorker(appContext: Context, workerParameters: WorkerParameters) :
  Worker(appContext, workerParameters) {
  companion object {
    const val PRINT_LABEL = "print_label"
    const val PRINTER_ADDRESS = "printer_address"
    const val PRINTER_PORT = "printer_port"
    const val WORK_RESULT = "result"
    const val PRINT_TASK = "print_task"

    fun createUniquePrintTask(
      printerAddress: String?,
      printerPort: Int,
      label: String
    ): OneTimeWorkRequest {

      if (printerAddress.isNullOrEmpty()) {
        throw InvalidParameterException("Endereço IP inválido")
      }

      if (printerPort == 0) {
        throw InvalidParameterException("Porta Inválida")
      }

      val data = Data.Builder().apply {
        putString(PRINT_LABEL, label)
        putString(PRINTER_ADDRESS, printerAddress)
        putInt(PRINTER_PORT, printerPort)
      }.build()

      return OneTimeWorkRequest
        .Builder(PrintWorker::class.java)
        .setInputData(data)
        .build()
    }
  }

  override fun doWork(): Result {
    val label = inputData.getString(PRINT_LABEL)
    val printerPort = inputData.getInt(PRINTER_PORT, 9100)
    val printerAddress = inputData.getString(PRINTER_ADDRESS) ?: return Result.failure(
      createDataMessage(applicationContext.getString(R.string.printer_ip_not_found))
    )

    val socket = try {
      Socket(printerAddress, printerPort)
    } catch (e: Exception) {
      return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }

    label?.apply {
      val outputStream = socket.getOutputStream()
      val pw = PrintWriter(outputStream)
      pw.write(this)
      pw.close()
      socket.close()
      return Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
    }

    return Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
  }

  private fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
  }

  override fun onStopped() {
    super.onStopped()
    Log.i("PrintWorker", "onStopped: Trabalho encerrado")
  }
}