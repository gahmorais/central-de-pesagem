package br.com.backupautomacao.centraldepesagem

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber
import java.io.PrintWriter
import java.net.Socket

class PrintWorker(appContext: Context, workerParameters: WorkerParameters) :
  Worker(appContext, workerParameters) {
  private val INTENT_ACTION_GRANT_USB = "${appContext.packageName}.GRANT_USB"

  companion object {
    const val PRINT_LABEL = "print_label"
    const val PRINTER_ADDRESS = "printer_address"
    const val PRINTER_PORT = "printer_port"
    const val USB_PRINTER_NAME = "usb_printer_name"
    const val WORK_RESULT = "result"
    const val PRINT_TASK = "print_task"

    fun createUniquePrintTask(
      printerType: String,
      printerAddress: String?,
      printerPort: Int,
      usbPrinterName: String?,
      label: String
    ): OneTimeWorkRequest {
      Timber.i("PrinterWorker - IP: $printerAddress PORT: $printerPort")
      val data: Data = if (printerType == "Usb" && !usbPrinterName.isNullOrEmpty()) {
        Data.Builder()
          .putString(PRINT_LABEL, label)
          .putString(USB_PRINTER_NAME, usbPrinterName)
          .build()
      } else if (printerType == "Network" && !printerAddress.isNullOrEmpty()) {
        Data.Builder()
          .putString(PRINT_LABEL, label)
          .putString(PRINTER_ADDRESS, printerAddress)
          .putInt(PRINTER_PORT, printerPort)
          .build()
      } else {
        throw Exception("Nenhuma impressora selecionada")
      }

      return OneTimeWorkRequestBuilder<PrintWorker>()
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

    val usbPrinterName = inputData.getString(USB_PRINTER_NAME).takeIf { !it.isNullOrEmpty() }
    val printerAddress = inputData.getString(PRINTER_ADDRESS).takeIf { !it.isNullOrEmpty() }
    val printerPort = inputData.getInt(PRINTER_PORT, 9100)

    return when {
      usbPrinterName != null -> printViaUsb(usbPrinterName, label)
      printerAddress != null -> printViaNetwork(printerAddress, printerPort, label)
      else -> Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }


  private fun printViaUsb(usbPrinterName: String?, label: String): Result {
    return try {
      val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
      val deviceList = usbManager.deviceList.values

      Timber.i("Dispositivos USB encontrados: " + deviceList.size)

      if (deviceList.isEmpty()) {
        Timber.e("Nenhum dispositivo USB encontrado.")
        return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
      }

      Timber.i("Procurando impressora com nome: $usbPrinterName")

      val usbDevice = deviceList.find { device ->
        Timber.i("Dispositivo encontrado: ${device.deviceName}, Produto: ${device.productName}")
        device.productName == usbPrinterName
      } ?: run {
        Timber.e("Impressora USB não encontrada.")
        return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
      }

      Timber.i("Impressora encontrada: " + usbDevice.deviceName)

      if (!usbManager.hasPermission(usbDevice)) {
        Timber.e("Permissão negada para o dispositivo USB: " + usbDevice.deviceName)
        val intent = Intent(INTENT_ACTION_GRANT_USB)
        val pendingIntent = PendingIntent.getBroadcast(
          applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(usbDevice, pendingIntent)
        return Result.failure(createDataMessage("Permissão USB negada. Tente novamente"))
      }

      Timber.i("Permissão concedida, tentando conectar...")

      val connection = usbManager.openDevice(usbDevice)
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_connection_failed)))

      Timber.i("Conexão com impressora estabelecida.")

      val interfaceIndex = 0
      val usbInterface = usbDevice.getInterface(interfaceIndex)
      val endpointOut = (0 until usbInterface.endpointCount)
        .map { usbInterface.getEndpoint(it) }
        .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.no_usb_endpoint)))

      Timber.i("Endpoint de saída identificado. Enviando dados...")

      connection.claimInterface(usbInterface, true)

      Timber.i("LabelTeste{$label}")
      val dataToSend = label.toByteArray(Charsets.UTF_8)
      val sentBytes = connection.bulkTransfer(endpointOut, dataToSend, dataToSend.size, 5000)

      connection.releaseInterface(usbInterface)
      connection.close()

      return if (sentBytes > 0) {
        Timber.i("Impressão realizada com sucesso.")
        Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
      } else {
        Timber.e("Erro ao imprimir, bytes enviados: $sentBytes")
        Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
      }
    } catch (e: Exception) {
      Timber.e(e, "Erro ao imprimir via USB")
      return Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
    }
  }

  private fun printViaNetwork(printerAddress: String, printerPort: Int, label: String): Result {
    return try {
      Socket(printerAddress, printerPort).use { socket ->
        PrintWriter(socket.getOutputStream()).use { pw ->
          pw.write(label)
          pw.flush()
        }
      }
      Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
    } catch (e: Exception) {
      Timber.e(e, "Erro ao imprimir via rede")
      Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
  }

  override fun onStopped() {
    super.onStopped()
    Timber.i("onStopped: Trabalho encerrado")
  }
}