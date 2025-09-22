package br.com.backupautomacao.pesagem_modular.printers


import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import br.com.backupautomacao.pesagem_modular.Constants
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.network.MgvProduct
import br.com.backupautomacao.pesagem_modular.utils.Formatter
import br.com.backupautomacao.pesagem_modular.utils.Formatter.Companion.formatNutriction
import br.com.backupautomacao.pesagem_modular.utils.LabelManager
import br.com.backupautomacao.pesagem_modular.utils.Printer
import br.com.backupautomacao.pesagem_modular.utils.UnitOfMeasure
import br.com.backupautomacao.pesagem_modular.utils.parcelable
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.io.IOException

class HoneywellPrinter(
  private val context: Context,
  private val usbManager: UsbManager,
  private val configRepository: ConfigRepository
) : Printer {

  private var usbDevice: UsbDevice? = null
  private var connection: UsbDeviceConnection? = null
  private val ACTION_USB_PERMISSION = "br.com.backupautomacao.USB_PERMISSION"
  private var onConnectionResult: ((Boolean, String) -> Unit)? = null

  var exibirImagens: Boolean = true

  override fun initializePrinter(device: UsbDevice) {
    if (usbDevice?.deviceId != device.deviceId) {
      disconnect()
    }
    usbDevice = device
  }

  fun device(deviceName: String): UsbDevice {
    val selectedDevice = usbManager.deviceList.values.firstOrNull { it.deviceName == deviceName }
      ?: throw Exception("Nenhum dispositivo encontrado")
    return selectedDevice
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
    } catch (_: Exception) {
    }

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
          } catch (_: Exception) {
          }
        }

        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
          val device = intent.parcelable<UsbDevice>(UsbManager.EXTRA_DEVICE)
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
      Timber.e("Dispositivo USB não definido")
      return false
    }

    if (!usbManager.hasPermission(usbDevice)) {
      Timber.e("Sem permissão para o dispositivo USB")
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

  private fun safeConnectOrRequestPermission(onReady: (Boolean, String) -> Unit) {
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


  fun print(product: MgvProduct, showQuantity: Boolean) {
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

  override fun print(product: MgvProduct) {
    val showQuantity = UnitOfMeasure.fromCodigo(product.unitOfMeasure) == UnitOfMeasure.UNIDADE
    print(product, showQuantity)
  }


  private fun buildZpl(context: Context, product: MgvProduct, showQuantity: Boolean): String {
    val selectedTemplateName = if (showQuantity) {
//      viewModel.getLabelTemplateQuant()
      configRepository.getTextConfig(context.getString(R.string.key_label_template_quant))
    } else {
      configRepository.getTextConfig(context.getString(R.string.key_label_template_kg))
//      viewModel.getLabelTemplateKg()
    }

    if (selectedTemplateName.isBlank()) {
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
//        val tara = "0"
//        val taraFormatada = if (showQuantity) "" else Formatter.formatWeight(tara)
    val quantidadeFormatada = if (showQuantity) (product.quantity ?: 0).toString() else ""
    val taraFormatada = Formatter.formatWeight(product.tare)

    val dataAtualMillis = System.currentTimeMillis()
    val dataEmbalado = Formatter.formatDate(dataAtualMillis)
    val diasValidade = product.shelfLife.takeIf { it > 0 } ?: 5
    val validadeMillis = dataAtualMillis + diasValidade * 24 * 60 * 60 * 1000
    val dataValidade = Formatter.formatDate(validadeMillis)

    val parteInteira = product.nutrictionFacts?.Parte_Inteira_Medida_Caseira.orEmpty().trim()
    val parteDecimal = product.nutrictionFacts?.Parte_Decimal_Medida_Caseira.orEmpty().trim()

    val medidaFormatada = when {
      parteInteira.isNotBlank() && parteDecimal.isNotBlank() -> "$parteInteira.$parteDecimal"
      parteInteira.isNotBlank() -> parteInteira
      parteDecimal.isNotBlank() -> "0.$parteDecimal"
      else -> ""
    }


    val nutrition = product.nutrictionFacts
    fun buildImagemZpl(): String {
      val imagens = mutableListOf<String>()
      if (exibirImagens && nutrition != null) {
        val acucar = nutrition.Alto_Acucar_Add == true
        val gordura = nutrition.Alto_Gordura_Saturada == true
        val sodio = nutrition.Alto_Sodio == true

        when {
          acucar && gordura && sodio -> imagens.add(Constants.IMAGE_ALL)
          acucar && gordura -> imagens.add(Constants.IMAGE_SUGAR_FAT)
          acucar && sodio -> imagens.add(Constants.IMAGE_SUGAR_SODIUM)
          gordura && sodio -> imagens.add(Constants.IMAGE_FAT_SODIUM)
          acucar -> imagens.add(Constants.IMAGE_ADDED_SUGAR)
          gordura -> imagens.add(Constants.IMAGE_SATURATED_FAT)
          sodio -> imagens.add(Constants.IMAGE_SODIUM)
        }
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

    val textoPorcoes = Formatter.formatPorcoes(
      product.weight,
      nutrition?.Quantidade_porcao,
      nutrition?.Unidade_porcao,
      nutrition?.Quantidade_Porcoes_Por_Embalagem
    )


    Timber.d("$product")
    template = template.replace("{{imagens_nutricionais}}", buildImagemZpl())
    val result = template
      .replace("var_descricao", product.description)
      .replace("var_codigo", barcode)
      .replace("var_preco_kilo", precoKilo)
      .replace("var_preco", precoTotal)
      .replace("var_peso", pesoFormatado)
      .replace("var_tara", taraFormatada)
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

      //INFORMAÇÃO NUTRICIONAL
      .replace("var_porcao_por_embalagem", textoPorcoes)
      .replace("var_porcao_por_unidade", product.nutrictionFacts?.Quantidade_porcao.orEmpty())
      .replace("var_unidade", product.nutrictionFacts?.Unidade_porcao.orEmpty())
      .replace("var_parte_inteira_medida", "")
      .replace("var_parte_decimal_medida", medidaFormatada)
      .replace("var_medida_utilizada", product.nutrictionFacts?.Medida_Caseira_Utilizada.orEmpty())

      // Nutricional (100g)
      .replace(
        "var_valor_energetico_100g",
        formatNutriction(nutrition?.ValorEnergetico_100g.orEmpty())
      )
      .replace("var_carboidratos_100g", formatNutriction(nutrition?.Carboidrato_100g.orEmpty()))
      .replace(
        "var_acucares_totais_100g",
        formatNutriction(nutrition?.Acucares_Totais_100g.orEmpty())
      )
      .replace(
        "var_acucares_adicionados_100g",
        formatNutriction(nutrition?.Acucares_Adicionado_100g.orEmpty())
      )
      .replace("var_proteinas_100g", formatNutriction(nutrition?.Proteina_100g.orEmpty()))
      .replace("var_gord_totais_100g", formatNutriction(nutrition?.Gordura_Totais_100g.orEmpty()))
      .replace(
        "var_gord_saturadas_100g",
        formatNutriction(nutrition?.Gordura_Saturada_100g.orEmpty())
      )
      .replace("var_gorduras_trans_100g", formatNutriction(nutrition?.GorduraTrans_100g.orEmpty()))
      .replace(
        "var_fibra_alimentar_100g",
        formatNutriction(nutrition?.FibraAlimentar_100g.orEmpty())
      )
      .replace("var_sodio_100g", formatNutriction(nutrition?.Sodio_100g.orEmpty()))

      // Nutricional (porção/ unidade)
      .replace(
        "var_valor_energetico_PU",
        formatNutriction(nutrition?.Porcao_ValorEnergetico.orEmpty())
      )
      .replace("var_carboidratos_PU", formatNutriction(nutrition?.Porcao_Carboidrato.orEmpty()))
      .replace(
        "var_acucares_totais_PU",
        formatNutriction(nutrition?.Porcao_AcucaresTotais.orEmpty())
      )
      .replace(
        "var_acucares_adicionados_PU",
        formatNutriction(nutrition?.Porcao_AcucaresAdicionado.orEmpty())
      )
      .replace("var_proteinas_PU", formatNutriction(nutrition?.Porcao_Proteina.orEmpty()))
      .replace("var_gord_totais_PU", formatNutriction(nutrition?.Porcao_GorduraTotais.orEmpty()))
      .replace(
        "var_gord_saturadas_PU",
        formatNutriction(nutrition?.Porcao_GorduraSaturada.orEmpty())
      )
      .replace("var_gorduras_trans_PU", formatNutriction(nutrition?.Porcao_GorduraTrans.orEmpty()))
      .replace(
        "var_fibra_alimentar_PU",
        formatNutriction(nutrition?.Porcao_FibraAlimentar.orEmpty())
      )
      .replace("var_sodio_PU", formatNutriction(nutrition?.Porcao_Sodio.orEmpty()))

      // Valores Diários PORCENTAGEM (%VD)
      .replace("vd_energetico", Formatter.formatVD(nutrition?.Porcentagem_vd_ValorEnergetico))
      .replace("vd_carboidratos", Formatter.formatVD(nutrition?.Porcentagem_vd_Carboidrato))
      .replace("vd_acucares_totais", Formatter.formatVD(nutrition?.Porcentagem_vd_AcucaresTotais))
      .replace(
        "vd_acucares_adicionados",
        Formatter.formatVD(nutrition?.Porcentagem_vd_AcucaresAdicionado)
      )
      .replace("vd_proteinas", Formatter.formatVD(nutrition?.Porcentagem_vd_Proteina))
      .replace("vd_gord_totais", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraTotais))
      .replace("vd_gord_saturadas", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraSaturada))
      .replace("vd_gord_trans", Formatter.formatVD(nutrition?.Porcentagem_vd_GorduraTrans))
      .replace("vd_fibra_alimentar", Formatter.formatVD(nutrition?.Porcentagem_vd_FibraAlimentar))
      .replace("vd_sodio", Formatter.formatVD(nutrition?.Porcentagem_vd_Sodio))

      .replace("var_copies", "1")

    return result
  }

}