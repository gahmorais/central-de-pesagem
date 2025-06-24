package br.com.backupautomacao.centraldepesagem.printers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import br.com.backupautomacao.centraldepesagem.network.MgvInformacoesNutricionais
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Printer
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import java.util.Random

class HoneywellPrinter(
    private val usbManager: UsbManager,
    private val context: Context
) : Printer {

    private var usbDevice: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private val ACTION_USB_PERMISSION = "br.com.backupautomacao.USB_PERMISSION"

    private var onConnectionResult: ((Boolean, String) -> Unit)? = null

   override fun initializePrinter(device: UsbDevice) {
        usbDevice = device
    }

    fun requestPermissionAndConnect(device: UsbDevice, onResult: (Boolean, String) -> Unit) {
        usbDevice = device
        onConnectionResult = onResult

        val permissionIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )

        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}

        val filter = IntentFilter(ACTION_USB_PERMISSION).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        usbManager.requestPermission(device, permissionIntent)
    }

    fun hasPermission(): Boolean {
        return usbDevice?.let { usbManager.hasPermission(it) } == true
    }

    fun connect(): Boolean {
        return connectPrinter()
    }


    fun printZpl(zpl: String) {
        val bytes = zpl.toByteArray(Charsets.UTF_8)
        val usbInterface = usbDevice?.getInterface(0)

        if (usbInterface == null || connection == null) {
            Timber.e("❌ Interface ou conexão nula")
            return
        }

        logUsbInterfaceDetails(usbDevice)

        val endpointOut = (0 until usbInterface.endpointCount)
            .map { usbInterface.getEndpoint(it) }
            .find {
                it.direction == UsbConstants.USB_DIR_OUT &&
                        it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
            }

        if (endpointOut == null) {
            Timber.e("❌ Nenhum endpoint de saída BULK encontrado")
            return
        }

        if (connection!!.claimInterface(usbInterface, true)) {
            val result = connection!!.bulkTransfer(endpointOut, bytes, bytes.size, 2000)
            Timber.d("📤 Resultado da transferência: $result")
            connection!!.releaseInterface(usbInterface)
        } else {
            Timber.e("❌ Falha ao clamar interface USB")
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted) {
                        val connected = connectPrinter()
                        onConnectionResult?.invoke(
                            connected,
                            if (connected) "Conectado com sucesso à impressora Honeywell"
                            else "Falha ao conectar na impressora Honeywell"
                        )
                    } else {
                        onConnectionResult?.invoke(false, "Permissão USB negada")
                    }
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (device == usbDevice) {
                        Timber.d("Dispositivo USB desconectado")
                        disconnect()
                        usbDevice = null
                    }
                }
            }
        }
    }

    private fun connectPrinter(): Boolean {
        if (usbDevice == null) {
            Timber.e("❌ Dispositivo USB não definido")
            return false
        }

        if (!usbManager.hasPermission(usbDevice)) {
            Timber.e("❌ Sem permissão para o dispositivo USB")
            return false
        }

        connection = usbManager.openDevice(usbDevice)
        return connection != null
    }

    suspend fun safeConnectSuspend(): Boolean = suspendCancellableCoroutine { cont ->
        safeConnectOrRequestPermission { success, _ ->
            cont.resume(success) {}
        }
    }

    fun safeConnectOrRequestPermission(onReady: (Boolean, String) -> Unit) {
        usbDevice?.let {
            if (usbManager.hasPermission(it)) {
                val connected = connectPrinter()
                onReady(connected, if (connected) "Conectado com sucesso" else "Falha na conexão")
            } else {
                requestPermissionAndConnect(it, onReady)
            }
        } ?: onReady(false, "Dispositivo USB não definido")
    }

    override fun disconnect() {
        try {
            usbDevice?.getInterface(0)?.let {
                connection?.releaseInterface(it)
            }
            connection?.close()
            Timber.d("✅ Conexão com a impressora fechada com sucesso")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao desconectar da impressora")
        } finally {
            connection = null
            usbDevice = null
        }
    }

    override fun print(product: MgvProduct) {
        if (connection == null && !connectPrinter()) {
            Timber.e("❌ Não foi possível conectar à impressora")
            return
        }

        val zpl = buildZpl(product)
        printZpl(zpl)
    }

    private fun buildZpl(product: MgvProduct): String {
        val totalPrice = product.price * (product.weight ?: 0.0)
        val barcode = createBarcode(product.code, product.weight ?: 0.0)
        val nutrition = product.nutrictionFacts?.let { createNutriction(it) } ?: ""

        return """
^XA
^MMC
^MTD
^MNN
^PW480
^FO2,193^GB475,0,4^FS
^FT0,184^A0N,34,33^FH\^CI28^FB480,1,,C^FDvar_descricao^FS^CI27
^FT12,244^A0N,28,28^FH\^CI28^FDData:^FS^CI27
^FT94,244^A0N,32,29^FH\^CI28^FDvar_data_embalado^FS^CI27
^BY3,2,90^FT98,474^BEN,,Y,N
^FH\^FDvar_codigo^FS
^FT12,290^A0N,28,28^FH\^CI28^FDPESO:^FS^CI27
^FT94,290^A0N,32,32^FH\^CI28^FDvar_peso^FS^CI27
^FT12,343^A0N,28,28^FH\^CI28^FDR${'$'}/kg:^FS^CI27
^FT94,343^A0N,32,32^FH\^CI28^FDvar_preco_kilo^FS^CI27
^FO288,286^GB175,64,1^FS
^FT320,335^A0N,34,34^FH\^CI28^FB112,1,,C^FDvar_preco^FS^CI27
^FT288,289^A0N,28,30^FH\^CI28^FD   TOTAL R${'$'}   ^FS^CI27
^LRY^FO288,261^GB175,0,35^FS^LRN
^PQvar_quantidade,0,1,Y
^XZ
        """.trimIndent()
    }

    private fun createBarcode(code: String, weight: Double): String {
        val paddedCode = code.padStart(6, '0')
        val weightString = String.format(Locale.ENGLISH, "%.0f", weight * 1000).padStart(5, '0')
        return "2$paddedCode$weightString"
    }

    private fun createNutriction(nutrition: MgvInformacoesNutricionais): String {
        val rnd = { "%.2f".format(Locale.ENGLISH, Random().nextDouble() * 100).replace(".", ",") }

        return """
            Valor Energético: ${rnd()}
            Carboidratos: ${rnd()}
            Açúcares: ${rnd()}
            Açúcares Adicionados: ${rnd()}
            Proteínas: ${rnd()}
            Gorduras Totais: ${rnd()}
            Gorduras Saturadas: ${rnd()}
            Gorduras Trans: ${rnd()}
            Fibra Alimentar: ${rnd()}
            Sódio: ${rnd()}
        """.trimIndent()
    }

    fun logUsbInterfaceDetails(usbDevice: UsbDevice?) {
        if (usbDevice == null) {
            Timber.e("❌ usbDevice é nulo")
            return
        }

        val interfaceCount = usbDevice.interfaceCount
        Timber.d("🔍 Total de interfaces disponíveis: $interfaceCount")

        for (interfaceIndex in 0 until interfaceCount) {
            val usbInterface = usbDevice.getInterface(interfaceIndex)

            Timber.d("🔍 Interface [$interfaceIndex]:")
            Timber.d("   ↪ ID: ${usbInterface.id}")
            Timber.d("   ↪ Classe: ${usbInterface.interfaceClass}")
            Timber.d("   ↪ SubClasse: ${usbInterface.interfaceSubclass}")
            Timber.d("   ↪ Protocolo: ${usbInterface.interfaceProtocol}")
            Timber.d("   ↪ Endpoints: ${usbInterface.endpointCount}")

            for (i in 0 until usbInterface.endpointCount) {
                val endpoint = usbInterface.getEndpoint(i)
                val direction = when (endpoint.direction) {
                    UsbConstants.USB_DIR_IN -> "IN"
                    UsbConstants.USB_DIR_OUT -> "OUT"
                    else -> "DESCONHECIDO"
                }
                val type = when (endpoint.type) {
                    UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                    UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                    UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                    UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
                    else -> "DESCONHECIDO"
                }

                Timber.d("     🛠️ Endpoint [$i]:")
                Timber.d("         ↪ Address: ${endpoint.address}")
                Timber.d("         ↪ Direction: $direction")
                Timber.d("         ↪ Type: $type")
                Timber.d("         ↪ MaxPacketSize: ${endpoint.maxPacketSize}")
                Timber.d("         ↪ Interval: ${endpoint.interval}")
            }
        }
    }
}