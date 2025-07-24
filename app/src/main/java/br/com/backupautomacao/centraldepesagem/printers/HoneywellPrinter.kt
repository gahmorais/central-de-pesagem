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
import androidx.core.content.ContentProviderCompat.requireContext
import br.com.backupautomacao.centraldepesagem.Constants
import br.com.backupautomacao.centraldepesagem.network.MgvInformacoesNutricionais
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import br.com.backupautomacao.centraldepesagem.utils.LabelManager
import br.com.backupautomacao.centraldepesagem.utils.Printer
import br.com.backupautomacao.centraldepesagem.utils.UnitOfMeasure
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.Random

class HoneywellPrinter(
    private val usbManager: UsbManager,
    private val context: Context,
    private val viewModel: SettingsViewModel
) : Printer {

    private var usbDevice: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private val ACTION_USB_PERMISSION = "br.com.backupautomacao.USB_PERMISSION"
    private var onConnectionResult: ((Boolean, String) -> Unit)? = null

    var exibirImagens: Boolean = true
//    override fun initializePrinter(device: UsbDevice) {
//        usbDevice = device
//    }

    override fun initializePrinter(device: UsbDevice) {
        if (usbDevice?.deviceId != device.deviceId) {
            disconnect()
        }
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

    private fun printZpl(zpl: String) {
        val bytes = zpl.toByteArray(Charsets.UTF_8)

        val usbInterface = usbDevice?.let { device ->
            (0 until device.interfaceCount)
                .map { device.getInterface(it) }
                .firstOrNull { iface ->
                    (0 until iface.endpointCount).any { ep ->
                        val endpoint = iface.getEndpoint(ep)
                        endpoint.direction == UsbConstants.USB_DIR_OUT &&
                                endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                    }
                }
        }

        if (usbInterface == null || connection == null) {
            Timber.e("❌ Interface ou conexão nula")
            return
        }

        val endpointOut = (0 until usbInterface.endpointCount)
            .map { usbInterface.getEndpoint(it) }
            .firstOrNull {
                it.direction == UsbConstants.USB_DIR_OUT &&
                        it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
            }

        if (endpointOut == null) {
            Timber.e("❌ Nenhum endpoint de saída BULK encontrado")
            return
        }

        try {
            if (connection!!.claimInterface(usbInterface, true)) {
                val result = connection!!.bulkTransfer(endpointOut, bytes, bytes.size, 2000)
                Timber.d("📤 Resultado da transferência: $result")
            } else {
                Timber.e("❌ Falha ao reivindicar interface USB")
            }
        } finally {
            connection!!.releaseInterface(usbInterface)
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


    fun print(context: Context, product: MgvProduct, showQuantity: Boolean) {
         if (connection == null) {
             val connected = connectPrinter()
             if (!connected || connection == null) {
                 Timber.e("❌ Conexão falhou ou nula após tentativa de conectar")
                 return
             }
         }

         val zpl = buildZpl(context, product, showQuantity)
        Timber.d("🖨️ --- ZPL GERADO ---\n$zpl\n🖨️ --- FIM DO ZPL ---")
        printZpl(zpl)
    }

//    override fun print(product: MgvProduct) {
//        val showQuantity = product.unitOfMeasure == "UN"
//        print(product, showQuantity)
//    }

    override fun print(product: MgvProduct) {
        val showQuantity = UnitOfMeasure.fromCodigo(product.unitOfMeasure) == UnitOfMeasure.UNIDADE
        print(context, product, showQuantity)
    }

    private fun buildZpl(context: Context, product: MgvProduct, showQuantity: Boolean): String {
        val selectedTemplateName = if (showQuantity) {
            viewModel.getLabelTemplateQuant()
        } else {
            viewModel.getLabelTemplateKg()
        }

        if (selectedTemplateName.isNullOrBlank()) {
            throw IllegalStateException("Nenhum template de etiqueta foi selecionado.")
        }

        val templateFile = File(
            LabelManager.getExternalLabelsDir(context),
            selectedTemplateName
        )

        if (!templateFile.exists()) {
            throw IOException("Template ZPL não encontrado: ${templateFile.absolutePath}")
        }


        var template = templateFile.readText(Charsets.UTF_8)

        val weight = product.weight ?: 0.0
        val unitPrice = if (product.promotionalPrice > 0) product.promotionalPrice else product.price
        val totalPrice = if (showQuantity) {
            unitPrice * (product.quantity ?: 0)
        } else {
            unitPrice * weight
        }
//        val tara = product.codTara ?: 0.0

        val barcode = Formatter.formatBarcode(product.code, totalPrice)
        val precoTotal = Formatter.formatPrice(totalPrice)
        val precoKilo = Formatter.formatPrice(unitPrice)
        val pesoFormatado = if (showQuantity) "" else Formatter.formatWeight(weight)
//        val taraFormatada = if (showQuantity) "" else Formatter.formatWeight(tara)
        val quantidadeFormatada = if (showQuantity) (product.quantity ?: 0).toString() else ""

        val dataAtualMillis = System.currentTimeMillis()
        val dataEmbalado = Formatter.formatDate(dataAtualMillis)
        val diasValidade = product.shelfLife.takeIf { it > 0 } ?: 5
        val validadeMillis = dataAtualMillis + diasValidade * 24 * 60 * 60 * 1000
        val dataValidade = Formatter.formatDate(validadeMillis)

        val nutrition = product.nutrictionFacts
        fun buildImagemZpl(): String {
            val imagens = mutableListOf<String>()
            if (exibirImagens) {
                if (nutrition?.Alto_Sodio == true) imagens.add(Constants.IMAGE_SODIUM)
                if (nutrition?.Alto_Gordura_Saturada == true) imagens.add(Constants.IMAGE_SATURATED_FAT)
                if (nutrition?.Alto_Acucar_Add == true) imagens.add(Constants.IMAGE_ADDED_SUGAR)
            }
            val startX = 100
            val spacing = 140
            val totalWidth = (imagens.size - 1) * spacing
            val offsetX = startX - (totalWidth / 2)
            return imagens.mapIndexed { index, imagemZpl ->
                val x = offsetX + (index * spacing)
                imagemZpl.replace("^FO\\d+,".toRegex(), "^FO${x},")
            }.joinToString("\n")
        }

        template = template.replace("{{imagens_nutricionais}}", buildImagemZpl())
        val result = template
            .replace("var_descricao", product.description)
            .replace("var_codigo", barcode)
            .replace("var_preco_kilo", precoKilo)
            .replace("var_preco", precoTotal)
            .replace("var_peso", pesoFormatado)
//            .replace("var_tara", taraFormatada)
            .replace("var_quant", quantidadeFormatada)
            .replace("var_data_embalado", dataEmbalado)
            .replace("var_data_validade", dataValidade)

            // Linhas extras
            .replace("var_primeira_linha", product.extraInfo?.line1.orEmpty())
            .replace("var_segunda_linha", product.extraInfo?.line2.orEmpty())
            .replace("var_terceira_linha", product.extraInfo?.line3.orEmpty())
            .replace("var_quarta_linha", product.extraInfo?.line4.orEmpty())
            .replace("var_quinta_linha", product.extraInfo?.line5.orEmpty())
            .replace("var_sexta_linha", product.extraInfo?.line6.orEmpty())
            .replace("var_setima_linha", product.extraInfo?.line7.orEmpty())
            .replace("var_oitava_linha", product.extraInfo?.line8.orEmpty())
            .replace("var_nona_linha", product.extraInfo?.line9.orEmpty())
            .replace("var_decima_linha", product.extraInfo?.line10.orEmpty())
            .replace("var_decima_primeira_linha", product.extraInfo?.line11.orEmpty())
            .replace("var_decima_segunda_linha", product.extraInfo?.line12.orEmpty())
            .replace("var_decima_terceira_linha", product.extraInfo?.line13.orEmpty())
            .replace("var_decima_quarta_linha", product.extraInfo?.line14.orEmpty())
            .replace("var_decima_quinta_linha", product.extraInfo?.line15.orEmpty())
            .replace("var_campoextra_line1", product.extraInfo?.campoextra.orEmpty())
            .replace("var_campoextra_line2", product.extraInfo?.campoextra2.orEmpty())
            .replace("var_campoextra_line3", product.extraInfo?.campoextra3.orEmpty())

            // Conservação
            .replace("var_conservacao_line1", product.conservacaoInfo?.conservacao.orEmpty())
            .replace("var_conservacao_line2", product.conservacaoInfo?.conservacao2.orEmpty())

            // Fracionador
            .replace("var_fracionador_line1", product.fracionadorInfo?.fracionador.orEmpty())
            .replace("var_fracionador_line2", product.fracionadorInfo?.fracionador2.orEmpty())

            // Fornecedor
            .replace("var_fornecedor_line1", product.fornecedorInfo?.fornecedor.orEmpty())
            .replace("var_fornecedor_line2", product.fornecedorInfo?.fornecedor2.orEmpty())

            // Nutricional (100g)
            .replace("var_valor_energetico_100g", Formatter.formatNutriction(nutrition?.ValorEnergetico_100g))
            .replace("var_carboidratos_100g", Formatter.formatNutriction(nutrition?.Carboidrato_100g))
            .replace("var_acucares_totais_100g", Formatter.formatNutriction(nutrition?.Acucares_Totais_100g))
            .replace("var_acucares_adicionados_100g", Formatter.formatNutriction(nutrition?.Acucares_Adicionado_100g))
            .replace("var_proteinas_100g", Formatter.formatNutriction(nutrition?.Proteina_100g))
            .replace("var_gord_totais_100g", Formatter.formatNutriction(nutrition?.Gordura_Totais_100g))
            .replace("var_gord_saturadas_100g", Formatter.formatNutriction(nutrition?.Gordura_Saturada_100g))
            .replace("var_gorduras_trans_100g", Formatter.formatNutriction(nutrition?.GorduraTrans_100g))
            .replace("var_fibra_alimentar_100g", Formatter.formatNutriction(nutrition?.FibraAlimentar_100g))
            .replace("var_sodio_100g", Formatter.formatNutriction(nutrition?.Sodio_100g))

            // VDs
            .replace("vd_energetico", Formatter.formatVD(nutrition?.Porcentagem_vd_ValorEnergetico.toString()))
            .replace("vd_carboidratos", Formatter.formatVD(nutrition?.Porcentagem_vd_Carboidrato))
            .replace("vd_acucares_totais", Formatter.formatVD(nutrition?.Porcentagem_vd_AcucaresTotais))
            .replace("vd_acucares_adicionados", Formatter.formatVD(nutrition?.Porcentagem_vd_AcucaresAdicionado))
            .replace("vd_proteinas", Formatter.formatVD(nutrition?.Porcentagem_vd_Proteina))
            .replace("vd_gord_totais", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraTotais))
            .replace("vd_gord_saturadas", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraSaturada))
            .replace("vd_gord_trans", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraTrans))
            .replace("vd_fibra_alimentar", Formatter.formatVD(nutrition?.Porcentagem_vd_FibraAlimentar))
            .replace("vd_sodio", Formatter.formatVD(nutrition?.Porcentagem_vd_Sodio))
            .replace("var_copies", "1")

        return result
    }


    private fun readAssetFile(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
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






/*
private fun buildZpl(product: MgvProduct, showQuantity: Boolean): String {
    val templateName = if (showQuantity) "labels/ET3085_QUANT.zpl" else "labels/ET3085_KG.zpl"
    var template = readAssetFile(templateName)

    val weight = product.weight ?: 0.0
    val unitPrice = if (product.promotionalPrice > 0) product.promotionalPrice else product.price
    val totalPrice = if (showQuantity) {
        unitPrice * (product.quantity ?: 0)
    } else {
        unitPrice * weight
    }
//        val tara = product.codTara ?: 0.0

    val barcode = Formatter.formatBarcode(product.code, totalPrice)
    val precoTotal = Formatter.formatPrice(totalPrice)
    val precoKilo = Formatter.formatPrice(unitPrice)
    val pesoFormatado = if (showQuantity) "" else Formatter.formatWeight(weight)
//        val taraFormatada = if (showQuantity) "" else Formatter.formatWeight(tara)
    val quantidadeFormatada = if (showQuantity) (product.quantity ?: 0).toString() else ""

    val dataAtualMillis = System.currentTimeMillis()
    val dataEmbalado = Formatter.formatDate(dataAtualMillis)
    val diasValidade = product.shelfLife.takeIf { it > 0 } ?: 5
    val validadeMillis = dataAtualMillis + diasValidade * 24 * 60 * 60 * 1000
    val dataValidade = Formatter.formatDate(validadeMillis)

    val nutrition = product.nutrictionFacts
    fun buildImagemZpl(): String {
        val imagens = mutableListOf<String>()
        if (exibirImagens) {
            if (nutrition?.Alto_Sodio == true) imagens.add(Constants.IMAGE_SODIUM)
            if (nutrition?.Alto_Gordura_Saturada == true) imagens.add(Constants.IMAGE_SATURATED_FAT)
            if (nutrition?.Alto_Acucar_Add == true) imagens.add(Constants.IMAGE_ADDED_SUGAR)
        }
        val startX = 100
        val spacing = 140
        val totalWidth = (imagens.size - 1) * spacing
        val offsetX = startX - (totalWidth / 2)
        return imagens.mapIndexed { index, imagemZpl ->
            val x = offsetX + (index * spacing)
            imagemZpl.replace("^FO\\d+,".toRegex(), "^FO${x},")
        }.joinToString("\n")
    }

    template = template.replace("{{imagens_nutricionais}}", buildImagemZpl())
    val result = template
        .replace("var_descricao", product.description)
        .replace("var_codigo", barcode)
        .replace("var_preco_kilo", precoKilo)
        .replace("var_preco", precoTotal)
        .replace("var_peso", pesoFormatado)
//            .replace("var_tara", taraFormatada)
        .replace("var_quant", quantidadeFormatada)
        .replace("var_data_embalado", dataEmbalado)
        .replace("var_data_validade", dataValidade)

        // Linhas extras
        .replace("var_primeira_linha", product.extraInfo?.line1.orEmpty())
        .replace("var_segunda_linha", product.extraInfo?.line2.orEmpty())
        .replace("var_terceira_linha", product.extraInfo?.line3.orEmpty())
        .replace("var_quarta_linha", product.extraInfo?.line4.orEmpty())
        .replace("var_quinta_linha", product.extraInfo?.line5.orEmpty())
        .replace("var_sexta_linha", product.extraInfo?.line6.orEmpty())
        .replace("var_setima_linha", product.extraInfo?.line7.orEmpty())
        .replace("var_oitava_linha", product.extraInfo?.line8.orEmpty())
        .replace("var_nona_linha", product.extraInfo?.line9.orEmpty())
        .replace("var_decima_linha", product.extraInfo?.line10.orEmpty())
        .replace("var_decima_primeira_linha", product.extraInfo?.line11.orEmpty())
        .replace("var_decima_segunda_linha", product.extraInfo?.line12.orEmpty())
        .replace("var_decima_terceira_linha", product.extraInfo?.line13.orEmpty())
        .replace("var_decima_quarta_linha", product.extraInfo?.line14.orEmpty())
        .replace("var_decima_quinta_linha", product.extraInfo?.line15.orEmpty())
        .replace("var_campoextra_line1", product.extraInfo?.campoextra.orEmpty())
        .replace("var_campoextra_line2", product.extraInfo?.campoextra2.orEmpty())
        .replace("var_campoextra_line3", product.extraInfo?.campoextra3.orEmpty())

        // Conservação
        .replace("var_conservacao_line1", product.conservacaoInfo?.conservacao.orEmpty())
        .replace("var_conservacao_line2", product.conservacaoInfo?.conservacao2.orEmpty())

        // Fracionador
        .replace("var_fracionador_line1", product.fracionadorInfo?.fracionador.orEmpty())
        .replace("var_fracionador_line2", product.fracionadorInfo?.fracionador2.orEmpty())

        // Fornecedor
        .replace("var_fornecedor_line1", product.fornecedorInfo?.fornecedor.orEmpty())
        .replace("var_fornecedor_line2", product.fornecedorInfo?.fornecedor2.orEmpty())

        // Nutricional (100g)
        .replace("var_valor_energetico_100g", Formatter.formatNutriction(nutrition?.ValorEnergetico_100g))
        .replace("var_carboidratos_100g", Formatter.formatNutriction(nutrition?.Carboidrato_100g))
        .replace("var_acucares_totais_100g", Formatter.formatNutriction(nutrition?.Acucares_Totais_100g))
        .replace("var_acucares_adicionados_100g", Formatter.formatNutriction(nutrition?.Acucares_Adicionado_100g))
        .replace("var_proteinas_100g", Formatter.formatNutriction(nutrition?.Proteina_100g))
        .replace("var_gord_totais_100g", Formatter.formatNutriction(nutrition?.Gordura_Totais_100g))
        .replace("var_gord_saturadas_100g", Formatter.formatNutriction(nutrition?.Gordura_Saturada_100g))
        .replace("var_gorduras_trans_100g", Formatter.formatNutriction(nutrition?.GorduraTrans_100g))
        .replace("var_fibra_alimentar_100g", Formatter.formatNutriction(nutrition?.FibraAlimentar_100g))
        .replace("var_sodio_100g", Formatter.formatNutriction(nutrition?.Sodio_100g))

        // VDs
        .replace("vd_energetico", Formatter.formatVD(nutrition?.Porcentagem_vd_ValorEnergetico.toString()))
        .replace("vd_carboidratos", Formatter.formatVD(nutrition?.Porcentagem_vd_Carboidrato))
        .replace("vd_acucares_totais", Formatter.formatVD(nutrition?.Porcentagem_vd_AcucaresTotais))
        .replace("vd_acucares_adicionados", Formatter.formatVD(nutrition?.Porcentagem_vd_AcucaresAdicionado))
        .replace("vd_proteinas", Formatter.formatVD(nutrition?.Porcentagem_vd_Proteina))
        .replace("vd_gord_totais", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraTotais))
        .replace("vd_gord_saturadas", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraSaturada))
        .replace("vd_gord_trans", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraTrans))
        .replace("vd_fibra_alimentar", Formatter.formatVD(nutrition?.Porcentagem_vd_FibraAlimentar))
        .replace("vd_sodio", Formatter.formatVD(nutrition?.Porcentagem_vd_Sodio))
        .replace("var_copies", "1")

    return result
}

 */




/*
private fun buildZpl(product: MgvProduct, showQuantity: Boolean): String {
    val templateName = if (showQuantity) "labels/ET3085_QUANT.zpl" else "labels/ET3085_KG.zpl"
    var template = readAssetFile(templateName)

    val weight = product.weight ?: 0.0
    val unitPrice = if (product.promotionalPrice > 0) product.promotionalPrice else product.price
    val totalPrice = if (showQuantity) {
        unitPrice * (product.quantity ?: 0)
    } else {
        unitPrice * (product.weight ?: 0.0)
    }
    val tara = product.codTara ?: 0.0

    val barcode = Formatter.formatBarcode(product.code, totalPrice)
    val precoTotal = Formatter.formatPrice(totalPrice)
    val precoKilo = Formatter.formatPrice(unitPrice)
    val pesoFormatado = if (showQuantity) "" else Formatter.formatWeight(weight)
    val taraFormatada = if (showQuantity) "" else Formatter.formatWeight(tara)
    val quantidadeFormatada = if (showQuantity) (product.quantity ?: 0).toString() else ""

    val dataAtualMillis = System.currentTimeMillis()
    val dataEmbalado = Formatter.formatDate(dataAtualMillis)
    val diasValidade = product.shelfLife.takeIf { it > 0 } ?: 5
    val validadeMillis = dataAtualMillis + diasValidade * 24 * 60 * 60 * 1000
    val dataValidade = Formatter.formatDate(validadeMillis)


    val nutrition = product.nutrictionFacts
    fun buildImagemZpl(): String {
        val imagens = mutableListOf<String>()

        if (exibirImagens) {
            if (nutrition?.Alto_Sodio?.let { it >= 100.0 } == true) {
                //600.0
                //300.0
                imagens.add(Constants.IMAGE_SODIUM)
                imagens.add(Constants.IMAGE_FORGET)
            }
            if (nutrition?.Alto_Gordura_Saturada?.let { it >= 3.0 } == true) {
                //6.0
                imagens.add(Constants.IMAGE_SATURATED_FAT)
                imagens.add(Constants.IMAGE_FORGET)
            }
            if (nutrition?.Alto_Acucar_Add?.let { it >= 7.5 } == true) {
                //15.00
                imagens.add(Constants.IMAGE_ADDED_SUGAR)
                imagens.add(Constants.IMAGE_FORGET)
            }
        }

        val startX = 100
        val spacing = 140
        val totalWidth = (imagens.size - 1) * spacing
        val offsetX = startX - (totalWidth / 2)

        val zplImagens = imagens.mapIndexed { index, imagemZpl ->
            val x = offsetX + (index * spacing)
            imagemZpl.replace("^FO\\d+,".toRegex(), "^FO${x},")
        }.joinToString("\n")

        return zplImagens
    }
    template = template.replace("{{imagens_nutricionais}}", buildImagemZpl())
    val hasNutrition = nutrition != null && listOf(
        nutrition.ValorEnergetico.toString(),
        nutrition.Carboidrato.toString(),
        nutrition.AcucaresTotais.toString(),
        nutrition.AcucaresAdicionado.toString(),
        nutrition.Proteina.toString(),
        nutrition.GorduraTotal.toString(),
        nutrition.GorduraSaturada.toString(),
        nutrition.GorduraTrans.toString(),
        nutrition.FibraAlimentar.toString(),
        nutrition.Sodio.toString()

    ).any { !it.isNullOrBlank() }

//        val valorEnergetico = if (hasNutrition) Formatter.formatNutriction(nutrition?.ValorEnergetico) else ""
//        val carboidratos = if (hasNutrition) Formatter.formatNutriction(nutrition?.Carboidrato) else ""
//        val acucaresTotais = if (hasNutrition) Formatter.formatNutriction(nutrition?.AcucaresTotais) else ""
//        val acucaresAdicionados = if (hasNutrition) Formatter.formatNutriction(nutrition?.AcucaresAdicionado) else ""
//        val proteinas = if (hasNutrition) Formatter.formatNutriction(nutrition?.Proteina) else ""
//        val gordurasTotais = if (hasNutrition) Formatter.formatNutriction(nutrition?.GorduraTotal) else ""
//        val gordurasSaturadas = if (hasNutrition) Formatter.formatNutriction(nutrition?.GorduraSaturada) else ""
//        val gordurasTrans = if (hasNutrition) Formatter.formatNutriction(nutrition?.GorduraTrans) else ""
//        val fibra = if (hasNutrition) Formatter.formatNutriction(nutrition?.FibraAlimentar) else ""
//        val sodio = if (hasNutrition) Formatter.formatNutriction(nutrition?.Sodio) else ""


    val valorEnergetico = Formatter.formatNutriction(nutrition?.ValorEnergetico)
    val carboidratos = Formatter.formatNutriction(nutrition?.Carboidrato)
    val acucaresTotais = Formatter.formatNutriction(nutrition?.AcucaresTotais)
    val acucaresAdicionados = Formatter.formatNutriction(nutrition?.AcucaresAdicionado)
    val proteinas = Formatter.formatNutriction(nutrition?.Proteina)
    val gordurasTotais = Formatter.formatNutriction(nutrition?.GorduraTotal)
    val gordurasSaturadas = Formatter.formatNutriction(nutrition?.GorduraSaturada)
    val gordurasTrans = Formatter.formatNutriction(nutrition?.GorduraTrans)
    val fibra = Formatter.formatNutriction(nutrition?.FibraAlimentar)
    val sodio = Formatter.formatNutriction(nutrition?.Sodio)

    val extra = product.extraInfo
    val extraLines = listOf(
        extra?.line1.orEmpty(),
        extra?.line2.orEmpty(),
        extra?.line3.orEmpty(),
        extra?.line4.orEmpty(),
        extra?.line5.orEmpty(),
        extra?.line6.orEmpty(),
        extra?.line7.orEmpty(),
        extra?.line8.orEmpty(),
        extra?.line9.orEmpty(),
        extra?.line10.orEmpty(),
        extra?.line11.orEmpty(),
        extra?.line12.orEmpty(),
        extra?.line13.orEmpty(),
        extra?.line14.orEmpty(),
        extra?.line15.orEmpty(),
        extra?.line16.orEmpty(),
        extra?.line17.orEmpty(),
        extra?.line18.orEmpty(),
        extra?.line19.orEmpty(),
        extra?.line20.orEmpty(),
        extra?.campoextra.orEmpty(),
        extra?.campoextra2.orEmpty(),
        extra?.campoextra3.orEmpty(),
    )

    var result = template
        .replace("var_descricao", product.description)
        .replace("var_codigo", barcode)
        .replace("var_preco_kilo", precoKilo)
        .replace("var_preco", precoTotal)
        .replace("var_peso", pesoFormatado)
        .replace("var_tara", taraFormatada)
        .replace("var_quant", quantidadeFormatada)
        .replace("var_data_embalado", dataEmbalado)
        .replace("var_data_validade", dataValidade)
        .replace("var_primeira_linha", product.extraInfo?.line1.orEmpty())
        .replace("var_segunda_linha", product.extraInfo?.line2.orEmpty())
        .replace("var_terceira_linha", product.extraInfo?.line3.orEmpty())
        .replace("var_quarta_linha", product.extraInfo?.line4.orEmpty())
        .replace("var_quinta_linha", product.extraInfo?.line5.orEmpty())
        .replace("var_sexta_linha", product.extraInfo?.line6.orEmpty())
        .replace("var_setima_linha", product.extraInfo?.line7.orEmpty())
        .replace("var_oitava_linha", product.extraInfo?.line8.orEmpty())
        .replace("var_nona_linha", product.extraInfo?.line9.orEmpty())
        .replace("var_decima_linha", product.extraInfo?.line10.orEmpty())
        .replace("var_decima_primeira_linha", product.extraInfo?.line11.orEmpty())
        .replace("var_decima_segunda_linha", product.extraInfo?.line12.orEmpty())
        .replace("var_decima_terceira_linha", product.extraInfo?.line13.orEmpty())
        .replace("var_decima_quarta_linha", product.extraInfo?.line14.orEmpty())
        .replace("var_decima_quinta_linha", product.extraInfo?.line15.orEmpty())
        .replace("var_campoextra_line1", product.extraInfo?.campoextra.orEmpty())
        .replace("var_campoextra_line2", product.extraInfo?.campoextra2.orEmpty())
        .replace("var_campoextra_line3", product.extraInfo?.campoextra3.orEmpty())
        .replace("var_conservacao_line1", product.conservacaoInfo?.conservacao.orEmpty())
        .replace("var_conservacao_line2", product.conservacaoInfo?.conservacao2.orEmpty())
        .replace("var_conservacao_line3", product.conservacaoInfo?.conservacao3.orEmpty())
        .replace("var_conservacao_line4", product.conservacaoInfo?.conservacao4.orEmpty())
        .replace("var_conservacao_line5", product.conservacaoInfo?.conservacao5.orEmpty())
        .replace("var_fracionador_line1", product.fracionadorInfo?.fracionador.orEmpty())
        .replace("var_fracionador_line2", product.fracionadorInfo?.fracionador2.orEmpty()
            .replace("var_fracionador_line3", product.fracionadorInfo?.fracionador3.orEmpty())
            .replace("var_fracionador_line4", product.fracionadorInfo?.fracionador4.orEmpty())
            .replace("var_fracionador_line5", product.fracionadorInfo?.fracionador5.orEmpty())
        .replace("var_fornecedor_line1", product.fornecedorInfo?.fornecedor.orEmpty())
        .replace("var_fornecedor_line2", product.fornecedorInfo?.fornecedor2.orEmpty())
            .replace("var_fornecedor_line3", product.fornecedorInfo?.fornecedor3.orEmpty())
            .replace("var_fornecedor_line4", product.fornecedorInfo?.fornecedor4.orEmpty())
            .replace("var_fornecedor_line5", product.fornecedorInfo?.fornecedor5.orEmpty())

        // Nutricionais (100g)
        .replace(
            "var_valor_energetico_100g",
            Formatter.formatNutriction(nutrition?.ValorEnergetico)
        )
        .replace("var_carboidratos_100g", Formatter.formatNutriction(nutrition?.Carboidrato))
        .replace("var_acucares_totais_100g", Formatter.formatNutriction(nutrition?.AcucaresTotais))
        .replace(
            "var_acucares_adicionados_100g",
            Formatter.formatNutriction(nutrition?.AcucaresAdicionado)
        )
        .replace("var_proteinas_100g", Formatter.formatNutriction(nutrition?.Proteina))
        .replace("var_gord_totais_100g", Formatter.formatNutriction(nutrition?.GorduraTotal))
        .replace("var_gord_saturadas_100g", Formatter.formatNutriction(nutrition?.GorduraSaturada))
        .replace("var_gorduras_trans_100g", Formatter.formatNutriction(nutrition?.GorduraTrans))
        .replace("var_fibra_alimentar_100g", Formatter.formatNutriction(nutrition?.FibraAlimentar))
        .replace("var_sodio_100g", Formatter.formatNutriction(nutrition?.Sodio))

        // Nutricionais (20g)
        .replace("var_valor_energetico_20g", Formatter.formatNutriction(nutrition?.ValorEnergetico))
        .replace("var_carboidratos_20g", Formatter.formatNutriction(nutrition?.Carboidrato))
        .replace("var_acucares_totais_20g", Formatter.formatNutriction(nutrition?.AcucaresTotais))
        .replace(
            "var_acucares_adicionados_20g",
            Formatter.formatNutriction(nutrition?.AcucaresAdicionado)
        )
        .replace("var_proteinas_20g", Formatter.formatNutriction(nutrition?.Proteina))
        .replace("var_gord_totais_20g", Formatter.formatNutriction(nutrition?.GorduraTotal))
        .replace("var_gord_saturadas_20g", Formatter.formatNutriction(nutrition?.GorduraSaturada))
        .replace("var_gorduras_trans_20g", Formatter.formatNutriction(nutrition?.GorduraTrans))
        .replace("var_fibra_alimentar_20g", Formatter.formatNutriction(nutrition?.FibraAlimentar))
        .replace("var_sodio_20g", Formatter.formatNutriction(nutrition?.Sodio))

        .replace(
            "vd_energetico",
            Formatter.calculateVD(nutrition?.ValorEnergetico?.toDouble(), 2000.0)
        )
        .replace("vd_carboidratos", Formatter.calculateVD(nutrition?.Carboidrato, 300.0))
        .replace("vd_acucares_totais", Formatter.calculateVD(nutrition?.AcucaresTotais, 50.0))
        .replace(
            "vd_acucares_adicionados",
            Formatter.calculateVD(nutrition?.AcucaresAdicionado, 50.0)
        )
        .replace("vd_proteinas", Formatter.calculateVD(nutrition?.Proteina, 75.0))
        .replace("vd_gord_totais", Formatter.calculateVD(nutrition?.GorduraTotal, 55.0))
        .replace("vd_gord_saturadas", Formatter.calculateVD(nutrition?.GorduraSaturada, 22.0))
        .replace("vd_gord_trans", Formatter.calculateVD(nutrition?.GorduraTrans, 20.0))
        .replace("vd_fibra_alimentar", Formatter.calculateVD(nutrition?.FibraAlimentar, 25.0))
        .replace("vd_sodio", Formatter.calculateVD(nutrition?.Sodio, 2400.0))
        .replace("var_copies", "1")
        )

    return result
}

 */