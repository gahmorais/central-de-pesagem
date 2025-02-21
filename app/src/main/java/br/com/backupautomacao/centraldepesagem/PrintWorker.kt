package br.com.backupautomacao.centraldepesagem

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.Socket
import java.security.InvalidParameterException
import kotlin.math.log

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
      printerAddress: String?,
      printerPort: Int,
      usbPrinterName: String?,
      label: String
    ): OneTimeWorkRequest {
      if (printerAddress.isNullOrEmpty() && usbPrinterName.isNullOrEmpty()) {
        throw InvalidParameterException("Nenhuma impressora válida configurada")
      }

      val data = Data.Builder().apply {
        putString(PRINT_LABEL, label)
        usbPrinterName?.let { putString(USB_PRINTER_NAME, it) }
        printerAddress?.let { putString(PRINTER_ADDRESS, it) }
        putInt(PRINTER_PORT, printerPort)
      }.build()

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

      Log.i("PrintWorker", "Dispositivos USB encontrados: ${deviceList.size}")

      if (deviceList.isEmpty()) {
        Log.e("PrintWorker", "Nenhum dispositivo USB encontrado.")
        return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
      }

      Log.i("PrintWorker", "Procurando impressora com nome: $usbPrinterName")

      val usbDevice = deviceList.find { device ->
        Log.i("PrintWorker", "Dispositivo encontrado: ${device.deviceName}, Produto: ${device.productName}")
        device.productName == usbPrinterName
      } ?: run {
        Log.e("PrintWorker", "Impressora USB não encontrada.")
        return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
      }

      Log.i("PrintWorker", "Impressora encontrada: ${usbDevice.deviceName}")

      if (!usbManager.hasPermission(usbDevice)) {
        Log.e("PrintWorker", "Permissão negada para o dispositivo USB: ${usbDevice.deviceName}")
        val intent = Intent(INTENT_ACTION_GRANT_USB)
        val pendingIntent = PendingIntent.getBroadcast(
          applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(usbDevice, pendingIntent)
        return Result.failure(createDataMessage("Permissão USB negada. Tente novamente"))
      }

      Log.i("PrintWorker", "Permissão concedida, tentando conectar...")

      val connection = usbManager.openDevice(usbDevice)
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_connection_failed)))

      Log.i("PrintWorker", "Conexão com impressora estabelecida.")

      val interfaceIndex = 0
      val usbInterface = usbDevice.getInterface(interfaceIndex)
      val endpointOut = (0 until usbInterface.endpointCount)
        .map { usbInterface.getEndpoint(it) }
        .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.no_usb_endpoint)))

      Log.i("PrintWorker", "Endpoint de saída identificado. Enviando dados...")

      connection.claimInterface(usbInterface, true)

      Log.i("PrintWorker", "LabelTeste{$label}")
      val dataToSend = label.toByteArray(Charsets.UTF_8)
      val sentBytes = connection.bulkTransfer(endpointOut, dataToSend, dataToSend.size, 5000)

      connection.releaseInterface(usbInterface)
      connection.close()

      return if (sentBytes > 0) {
        Log.i("PrintWorker", "Impressão realizada com sucesso.")
        Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
      } else {
        Log.e("PrintWorker", "Erro ao imprimir, bytes enviados: $sentBytes")
        Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
      }
    } catch (e: Exception) {
      Log.e("PrintWorker", "Erro ao imprimir via USB", e)
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
      Log.e("PrintWorker", "Erro ao imprimir via rede", e)
      Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
  }

  override fun onStopped() {
    super.onStopped()
    Log.i("PrintWorker", "onStopped: Trabalho encerrado")
  }
}







/*
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.Socket
import java.security.InvalidParameterException
import kotlin.math.log

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
      printerAddress: String?,
      printerPort: Int,
      usbPrinterName: String?,
      label: String
    ): OneTimeWorkRequest {
      if (printerAddress.isNullOrEmpty() && usbPrinterName.isNullOrEmpty()) {
        throw InvalidParameterException("Nenhuma impressora válida configurada")
      }

      val data = Data.Builder().apply {
        putString(PRINT_LABEL, label)
        usbPrinterName?.let { putString(USB_PRINTER_NAME, it) }
        printerAddress?.let { putString(PRINTER_ADDRESS, it) }
        putInt(PRINTER_PORT, printerPort)
      }.build()

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

//  override fun doWork(): Result {
//    val label = inputData.getString(PRINT_LABEL)
//      ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
//    val usbPrinterName = inputData.getString(USB_PRINTER_NAME)
//    val printerAddress = inputData.getString(PRINTER_ADDRESS)
//    val printerPort = inputData.getInt(PRINTER_PORT, 9100)
//
//    return when {
//      !usbPrinterName.isNullOrEmpty() -> printViaUsb(usbPrinterName, label)
//      !printerAddress.isNullOrEmpty() -> printViaNetwork(printerAddress, printerPort, label)
//      else -> Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
//    }
//  }

  /*
  if (usbDevice == null) {
        Log.e("PrintWorker", "Impressora USB não encontrada")
        return Result.failure(createDataMessage("Impressora USB não encontrada"))
      }
      else{
        val intent = Intent(INTENT_ACTION_GRANT_USB)
        val pendingIntent = PendingIntent.getBroadcast(
          applicationContext,
          0,
          intent,
          PendingIntent.FLAG_IMMUTABLE
        )

        usbManager.requestPermission(usbDevice, pendingIntent)

      }
   */

  private fun printViaUsb(usbPrinterName: String?, label: String): Result {
    return try {
      val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
      val deviceList = usbManager.deviceList.values

      Log.i("PrintWorker", "Dispositivos USB encontrados: ${deviceList.size}")

      if (deviceList.isEmpty()) {
        Log.e("PrintWorker", "Nenhum dispositivo USB encontrado.")
        return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
      }

      Log.i("PrintWorker", "Procurando impressora com nome: $usbPrinterName")

      val usbDevice = deviceList.find { device ->
        Log.i("PrintWorker", "Dispositivo encontrado: ${device.deviceName}, Produto: ${device.productName}")
        device.productName == usbPrinterName
      } ?: run {
        Log.e("PrintWorker", "Impressora USB não encontrada.")
        return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
      }

      Log.i("PrintWorker", "Impressora encontrada: ${usbDevice.deviceName}")

      if (!usbManager.hasPermission(usbDevice)) {
        Log.e("PrintWorker", "Permissão negada para o dispositivo USB: ${usbDevice.deviceName}")
        val intent = Intent(INTENT_ACTION_GRANT_USB)
        val pendingIntent = PendingIntent.getBroadcast(
          applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(usbDevice, pendingIntent)
        return Result.failure(createDataMessage("Permissão USB negada. Tente novamente"))
      }

      Log.i("PrintWorker", "Permissão concedida, tentando conectar...")

      val connection = usbManager.openDevice(usbDevice)
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_connection_failed)))

      Log.i("PrintWorker", "Conexão com impressora estabelecida.")

      val interfaceIndex = 0
      val usbInterface = usbDevice.getInterface(interfaceIndex)
      val endpointOut = (0 until usbInterface.endpointCount)
        .map { usbInterface.getEndpoint(it) }
        .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.no_usb_endpoint)))

      Log.i("PrintWorker", "Endpoint de saída identificado. Enviando dados...")

      connection.claimInterface(usbInterface, true)

      Log.i("PrintWorker", "LabelTeste{$label}")
      val dataToSend = label.toByteArray(Charsets.UTF_8)
      val sentBytes = connection.bulkTransfer(endpointOut, dataToSend, dataToSend.size, 5000)

      connection.releaseInterface(usbInterface)
      connection.close()

      return if (sentBytes > 0) {
        Log.i("PrintWorker", "Impressão realizada com sucesso.")
        Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
      } else {
        Log.e("PrintWorker", "Erro ao imprimir, bytes enviados: $sentBytes")
        Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
      }
    } catch (e: Exception) {
      Log.e("PrintWorker", "Erro ao imprimir via USB", e)
      return Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
    }
  }


  /*
  private fun printViaUsb(usbPrinterName: String?, label: String): Result {
    return try {
      val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
      Log.i("Teste", usbManager.deviceList.toString())
      val usbDevice = usbManager.deviceList.values.find { device ->
        Log.i("Teste", device.productName.toString())
        device.productName == usbPrinterName }
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))

//      if (!usbManager.hasPermission(usbDevice)) {
//        return Result.failure(createDataMessage(applicationContext.getString(R.string.no_usb_permission)))
//      }
//      if (usbDevice == null) {
      if (!usbManager.hasPermission(usbDevice)){
        Log.e("PrintWorker", "Impressora USB não encontrada")
        return Result.failure(createDataMessage("Impressora USB não encontrada"))
      }
      else{
        val intent = Intent(INTENT_ACTION_GRANT_USB)
        val pendingIntent = PendingIntent.getBroadcast(
          applicationContext,
          0,
          intent,
          PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(usbDevice, pendingIntent)
      }

//      if (!usbManager.hasPermission(usbDevice)) {
//        return Result.failure(createDataMessage(applicationContext.getString(R.string.no_usb_permission)))
//      }

      val connection = usbManager.openDevice(usbDevice)
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_connection_failed)))

      val interfaceIndex = 0
      val usbInterface = usbDevice.getInterface(interfaceIndex)
      val endpointOut = (0 until usbInterface.endpointCount)
        .map { usbInterface.getEndpoint(it) }
        .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.no_usb_endpoint)))

      connection.claimInterface(usbInterface, true)

      val dataToSend = label.toByteArray(Charsets.UTF_8)
//      val dataToSend = "^XA~WC^XZ".toByteArray()
      val sentBytes = connection.bulkTransfer(endpointOut, dataToSend, dataToSend.size, 5000)

      connection.releaseInterface(usbInterface)
      connection.close()

      if (sentBytes > 0) {
        Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
      } else {
        Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
      }
    } catch (e: Exception) {
      Log.e("PrintWorker", "Erro ao imprimir via USB", e)
      Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
    }
  }
*/
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
      Log.e("PrintWorker", "Erro ao imprimir via rede", e)
      Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
  }

  override fun onStopped() {
    super.onStopped()
    Log.i("PrintWorker", "onStopped: Trabalho encerrado")
  }
}
 */









/*
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.Socket
import java.security.InvalidParameterException

class PrintWorker(appContext: Context, workerParameters: WorkerParameters) :
  Worker(appContext, workerParameters) {

  companion object {
    const val PRINT_LABEL = "print_label"
    const val PRINTER_ADDRESS = "printer_address"
    const val PRINTER_PORT = "printer_port"
    const val USB_PRINTER_NAME = "usb_printer_name"
    const val WORK_RESULT = "result"
    const val PRINT_TASK = "print_task"

    fun createUniquePrintTask(
      printerAddress: String?,
      printerPort: Int,
      usbPrinterName: String?,
      label: String
    ): OneTimeWorkRequest {
      if (printerAddress.isNullOrEmpty() && usbPrinterName.isNullOrEmpty()) {
        throw InvalidParameterException("Nenhuma impressora válida configurada")
      }

      val data = Data.Builder().apply {
        putString(PRINT_LABEL, label)
        usbPrinterName?.let { putString(USB_PRINTER_NAME, it) }
        printerAddress?.let { putString(PRINTER_ADDRESS, it) }
        putInt(PRINTER_PORT, printerPort)
      }.build()

      return OneTimeWorkRequestBuilder<PrintWorker>()
        .setInputData(data)
        .build()
    }
  }

  override fun doWork(): Result {
    val label = inputData.getString(PRINT_LABEL)
      ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
    val usbPrinterName = inputData.getString(USB_PRINTER_NAME)
    val printerAddress = inputData.getString(PRINTER_ADDRESS)
    val printerPort = inputData.getInt(PRINTER_PORT, 9100)

    return when {
      !usbPrinterName.isNullOrEmpty() -> printViaUsb(usbPrinterName, label)
      !printerAddress.isNullOrEmpty() -> printViaNetwork(printerAddress, printerPort, label)
      else -> Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun printViaUsb(printerName: String, label: String): Result {
    return try {
      val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
      val usbDevice = usbManager.deviceList.values.find { it.deviceName == printerName }
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))

      val connection = usbManager.openDevice(usbDevice)
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_connection_failed)))

      val parcelFileDescriptor = ParcelFileDescriptor.fromFd(connection.fileDescriptor)

      FileOutputStream(parcelFileDescriptor.fileDescriptor).use { outputStream ->
        outputStream.write(label.toByteArray())
        outputStream.flush()
      }
      parcelFileDescriptor.close()
      connection.close()

      Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
    } catch (e: Exception) {
      Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
    }
  }

//  private fun printViaUsb(printerName: String, label: String): Result {
//    return try {
//      val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
//      val usbDevice = usbManager.deviceList.values.find { it.deviceName == printerName }
//        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
//
//      if (!usbManager.hasPermission(usbDevice)) {
//        return Result.failure(createDataMessage(applicationContext.getString(R.string.no_usb_permission)))
//      }
//
//      val connection = usbManager.openDevice(usbDevice)
//        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_connection_failed)))
//
//      val interfaceIndex = 0
//      val usbInterface = usbDevice.getInterface(interfaceIndex)
//      val endpointOut = (0 until usbInterface.endpointCount)
//        .map { usbInterface.getEndpoint(it) }
//        .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
//        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.no_usb_endpoint)))
//
//      connection.claimInterface(usbInterface, true)
//
//      val dataToSend = label.toByteArray(Charsets.UTF_8)
//      val sentBytes = connection.bulkTransfer(endpointOut, dataToSend, dataToSend.size, 5000)
//
//      connection.releaseInterface(usbInterface)
//      connection.close()
//
//      if (sentBytes > 0) {
//        Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
//      } else {
//        Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
//      }
//    } catch (e: Exception) {
//      Log.e("PrintWorker", "Erro ao imprimir via USB", e)
//      Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
//    }
//  }

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
      Log.e("PrintWorker", "Erro ao imprimir via rede", e)
      Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
  }

  override fun onStopped() {
    super.onStopped()
    Log.i("PrintWorker", "onStopped: Trabalho encerrado")
  }
}

 */





/*
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.Socket
import java.security.InvalidParameterException


class PrintWorker(appContext: Context, workerParameters: WorkerParameters) :
  Worker(appContext, workerParameters) {

  companion object {
    const val PRINT_LABEL = "print_label"
    const val PRINTER_ADDRESS = "printer_address"
    const val PRINTER_PORT = "printer_port"
    const val USB_PRINTER_NAME = "usb_printer_name"
    const val WORK_RESULT = "result"
    const val PRINT_TASK = "print_task"

    fun createUniquePrintTask(
      printerAddress: String?,
      printerPort: Int,
      usbPrinterName: String?,
      label: String
    ): OneTimeWorkRequest {
      if (printerAddress.isNullOrEmpty() && usbPrinterName.isNullOrEmpty()) {
        throw InvalidParameterException("Nenhuma impressora válida configurada")
      }

      val data = Data.Builder().apply {
        putString(PRINT_LABEL, label)
        usbPrinterName?.let { putString(USB_PRINTER_NAME, it) }
        printerAddress?.let { putString(PRINTER_ADDRESS, it) }
        putInt(PRINTER_PORT, printerPort)
      }.build()

      return OneTimeWorkRequestBuilder<PrintWorker>()
        .setInputData(data)
        .build()
    }
  }

  override fun doWork(): Result {
    val label = inputData.getString(PRINT_LABEL)
      ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
    val usbPrinterName = inputData.getString(USB_PRINTER_NAME)
    val printerAddress = inputData.getString(PRINTER_ADDRESS)
    val printerPort = inputData.getInt(PRINTER_PORT, 9100)

    return when {
      !usbPrinterName.isNullOrEmpty() -> printViaUsb(usbPrinterName, label)
      !printerAddress.isNullOrEmpty() -> printViaNetwork(printerAddress, printerPort, label)
      else -> Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun printViaUsb(printerName: String, label: String): Result {
    return try {
      val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
      val usbDevice = usbManager.deviceList.values.find { it.deviceName == printerName }
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))

      val connection = usbManager.openDevice(usbDevice)
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_connection_failed)))

      ParcelFileDescriptor.fromFd(connection.fileDescriptor).use { parcelFileDescriptor ->
        FileOutputStream(parcelFileDescriptor.fileDescriptor).use { outputStream ->
          outputStream.write(label.toByteArray())
          outputStream.flush()
        }
      }

      connection.close()
      Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
    } catch (e: Exception) {
      Log.e("PrintWorker", "Erro ao imprimir via USB", e)
      Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
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
      Log.e("PrintWorker", "Erro ao imprimir via rede", e)
      Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
  }

  override fun onStopped() {
    super.onStopped()
    Log.i("PrintWorker", "onStopped: Trabalho encerrado")
  }
}

 */



/*
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.Socket
import java.security.InvalidParameterException

class PrintWorker(appContext: Context, workerParameters: WorkerParameters) :
  Worker(appContext, workerParameters) {

  companion object {
    const val PRINT_LABEL = "print_label"
    const val PRINTER_ADDRESS = "printer_address"
    const val PRINTER_PORT = "printer_port"
    const val USB_PRINTER_NAME = "usb_printer_name"
    const val WORK_RESULT = "result"
    const val PRINT_TASK = "print_task"

    fun createUniquePrintTask(
      printerAddress: String?,
      printerPort: Int,
      usbPrinterName: String?,
      label: String
    ): OneTimeWorkRequest {

      if (printerAddress.isNullOrEmpty() && usbPrinterName.isNullOrEmpty()) {
        throw InvalidParameterException("Nenhuma impressora válida configurada")
      }

      val data = Data.Builder().apply {
        putString(PRINT_LABEL, label)
        usbPrinterName?.let { putString(USB_PRINTER_NAME, it) }
        printerAddress?.let { putString(PRINTER_ADDRESS, it) }
        putInt(PRINTER_PORT, printerPort)
      }.build()

      return OneTimeWorkRequest
        .Builder(PrintWorker::class.java)
        .setInputData(data)
        .build()
    }
  }

  override fun doWork(): Result {
    val label = inputData.getString(PRINT_LABEL) ?: return Result.failure(
      createDataMessage(applicationContext.getString(R.string.printing_error))
    )

    val usbPrinterName = inputData.getString(USB_PRINTER_NAME)
    val printerAddress = inputData.getString(PRINTER_ADDRESS)
    val printerPort = inputData.getInt(PRINTER_PORT, 9100)

    return if (!usbPrinterName.isNullOrEmpty()) {
      printViaUsb(usbPrinterName, label)
    } else if (!printerAddress.isNullOrEmpty()) {
      printViaNetwork(printerAddress, printerPort, label)
    } else {
      Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun printViaUsb(printerName: String, label: String): Result {
    return try {
      val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
      val usbDevice = usbManager.deviceList.values.find { it.deviceName == printerName }
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))

      val connection = usbManager.openDevice(usbDevice)
        ?: return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_connection_failed)))

      val parcelFileDescriptor = ParcelFileDescriptor.fromFd(connection.fileDescriptor)

      FileOutputStream(parcelFileDescriptor.fileDescriptor).use { outputStream ->
        outputStream.write(label.toByteArray())
        outputStream.flush()
      }
      parcelFileDescriptor.close()
      connection.close()

      Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
    } catch (e: Exception) {
      Result.failure(createDataMessage(applicationContext.getString(R.string.printing_error)))
    }
  }

  private fun printViaNetwork(printerAddress: String, printerPort: Int, label: String): Result {
    return try {
      val socket = Socket(printerAddress, printerPort)
      val outputStream = socket.getOutputStream()
      val pw = PrintWriter(outputStream)

      pw.write(label)
      pw.flush()
      pw.close()
      socket.close()

      Result.success(createDataMessage(applicationContext.getString(R.string.printing_done)))
    } catch (e: Exception) {
      Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
    }
  }

  private fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
  }

  override fun onStopped() {
    super.onStopped()
    Log.i("PrintWorker", "onStopped: Trabalho encerrado")
  }
}
*/

//Original
/*
import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
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
}*/