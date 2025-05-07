package br.com.backupautomacao.centraldepesagem.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import br.com.backupautomacao.centraldepesagem.R
import timber.log.Timber

class UsbWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {
    private val INTENT_ACTION_GRANT_USB = "${context.packageName}.GRANT_USB"

    companion object {
        const val USB_PRINTER_NAME = "usb_printer_name"
        fun createUniquePrintTask(
            usbPrinterName: String?,
            label: String
        ): OneTimeWorkRequest {

            val data: Data = Data.Builder()
                .putString(PRINT_LABEL, label)
                .putString(USB_PRINTER_NAME, usbPrinterName)
                .build()

            return OneTimeWorkRequestBuilder<UsbWorker>()
                .setInputData(data)
                .build()
        }
    }


    override suspend fun doWork(): Result {
        val label = inputData.getString(PRINT_LABEL) ?: return Result.failure(
            createDataMessage(
                applicationContext.getString(R.string.printing_error)
            )
        )

        val usbPrinterName = inputData.getString(USB_PRINTER_NAME) ?: return Result.failure(
            createDataMessage("Impressora USB inválida")
        )
        return sendingDataToUsb(usbPrinterName, label)
    }

    private fun sendingDataToUsb(usbPrinterName: String?, label: String): Result {
        return try {
            val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceList = usbManager.deviceList.values

            Timber.i("Dispositivos USB encontrados: %s", deviceList.size)

            if (deviceList.isEmpty()) {
                Timber.e("Nenhum dispositivo USB encontrado.")
                return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
            }

            Timber.i("Procurando impressora com nome: $usbPrinterName")

            val usbDevice = deviceList.find { device ->
                Timber.i("Dispositivo encontrado: ${device.deviceName}, Produto: ${device.productName}")
                device.productName == usbPrinterName
            }

            if (usbDevice == null) {
                Timber.e("Impressora USB não encontrada.")
                return Result.failure(createDataMessage(applicationContext.getString(R.string.printer_not_found)))
            }

            Timber.i("Impressora encontrada: %s", usbDevice.deviceName)

            if (!usbManager.hasPermission(usbDevice)) {
                Timber.e("Permissão negada para o dispositivo USB: %s", usbDevice.deviceName)
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
}