package br.com.backupautomacao.centraldepesagem

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.withTimeout
import java.io.PrintWriter
import java.net.Socket
import java.time.Duration
import java.util.concurrent.TimeUnit

class PrintWorker(appContext: Context, workerParameters: WorkerParameters) :
  Worker(appContext, workerParameters) {
  companion object {
    const val PRINT_LABEL = "print_label"
    const val PRINTER_ADDRESS = "printer_address"
    const val PRINTER_PORT = "printer_port"
    const val WORK_RESULT = "result"
    const val PRINT_TASK = "print_task"

    fun createUniquePrintTask(
      printerAddress: String,
      printerPort: Int,
      label: String
    ) : OneTimeWorkRequest {
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
      createDataMessage("IP da impressora não encontrado")
    )

    val socket = try {
      Socket(printerAddress, printerPort)
    } catch (e: Exception) {
      return Result.failure(createDataMessage("Falha ao buscar impressora"))
    }

    label?.apply {
      val outputStream = socket.getOutputStream()
      val pw = PrintWriter(outputStream)
      pw.write(this)
      pw.close()
      socket.close()
      return Result.success(createDataMessage("Impressão realizada"))
    }
    return Result.failure(createDataMessage("Falha ao realizar a impressão"))
  }

  private fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
  }

  override fun onStopped() {
    super.onStopped()
    Log.i("PrintWorker", "onStopped: Trabalho encerrado")
  }


}