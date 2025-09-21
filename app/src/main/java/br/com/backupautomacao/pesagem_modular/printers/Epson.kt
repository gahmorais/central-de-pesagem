package br.com.backupautomacao.pesagem_modular.printers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import br.com.backupautomacao.pesagem_modular.network.MgvInformacoesNutricionais
import br.com.backupautomacao.pesagem_modular.network.MgvProduct
import br.com.backupautomacao.pesagem_modular.utils.Formatter
import com.epson.epos2.discovery.DeviceInfo
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import timber.log.Timber
import java.util.Locale
import kotlin.random.Random

class Epson(
  private val usbManager: UsbManager,
  private val context: Context
) : DiscoveryListener, ReceiveListener {
  private var printer: Printer? = null
  private lateinit var printerPort: String
  private lateinit var printerName: String
  private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

  fun initializePrinter(usbConfig: String) {
    val (printerName, printerPort) = usbConfig.split(";")
    val printerModel = getPrinterModel(printerName)
    this.printerPort = printerPort
    printer = Printer(printerModel, Printer.LANG_EN, context)
    printer?.setReceiveEventListener(this)
  }

  fun finalizePrinter() {
    printer?.setReceiveEventListener(null)
    printer = null
  }

  private fun createDescription(description: String): String {
    val textData = StringBuilder()
    textData.append("$description\n")
    textData.append("------------------------------\n")
    return textData.toString()
  }

  private fun createWeight(weight: Double): String {
    val textData = StringBuilder()
    val weightFormatted = Formatter.formatWeight(weight)
    val weightWithPadding = weightFormatted.padStart(30, ' ')
    textData.append("Peso: $weightWithPadding\n")
    return textData.toString()
  }

  private fun createPricePerKg(price: Double): String {
    val textData = StringBuilder()
    val pricePerKg = Formatter.formatPrice(price)
    val priceText = "Preço/Kg"
    val priceWithPadding = "R$ $pricePerKg".padStart(30, ' ')
    textData.append("$priceText$priceWithPadding\n")

    return textData.toString()
  }

  private fun formatBoldText() {
    printer?.addTextFont(Printer.FONT_B)
    printer?.addTextSize(2, 2)
  }

  private fun formatDefaultText() {
    printer?.addTextFont(Printer.FONT_A)
    printer?.addTextSize(1, 1)
  }

  private fun createTotalPrice(price: Double, weight: Double): String {
    val textData = StringBuilder()
    val totalPrice = weight.times(price)
    val totalPriceFormatted = Formatter.formatPrice(totalPrice)
    val totalPriceText = "TOTAL R$"
    val totalPricePadding = "$totalPriceText $totalPriceFormatted".padStart(10, ' ')
    textData.append("$totalPricePadding\n")
    return textData.toString()
  }

  private fun createBarcode(productCode: String, productWeight: Double): String {
    val code = productCode.padStart(6, '0')
    val weight = productWeight.times(1000)
    val weightString = String.format(Locale.ENGLISH, "%.0f", weight).padStart(5, '0')
    return "2$code$weightString"
  }

  private fun createNutriction(nutrition: MgvInformacoesNutricionais): String {
    val textData = StringBuilder()

    val valorEnergetico = randomValue()
    val carboidratosTotais = randomValue()
    val acucaresTotais = randomValue()
    val acucaresAdicionados = randomValue()
    val proteinas = randomValue()
    val gordurasTotais = randomValue()
    val gordurasSaturadas = randomValue()
    val gordurasTrans = randomValue()
    val fibraAlimentar = randomValue()
    val sodio = randomValue()

    textData.append("Valor Energético........$valorEnergetico\n")
    textData.append("Carboidratos............$carboidratosTotais\n")
    textData.append("Acucares totais.........$acucaresTotais\n")
    textData.append(" Acucares adicionados...$acucaresAdicionados\n")
    textData.append("Proteínas...............$proteinas\n")
    textData.append("Gorduras totais.........$gordurasTotais\n")
    textData.append(" Gorduras saturadas.....$gordurasSaturadas\n")
    textData.append(" Gorduras trans.........$gordurasTrans\n")
    textData.append("Fibra Alimentar.........$fibraAlimentar\n")
    textData.append("Sódio...................$sodio\n")

    return textData.toString()
  }

  private fun randomValue(): String {
    val value = Random.nextDouble(from = 0.0, until = 99.9)
    val valueFormatted = String.format(Locale.ENGLISH, "%.2f", value)
      .replace(".", ",")
    return valueFormatted
  }

  private fun createDescription(): String {
    val textData = StringBuilder()
    val infoDescription = "---INFORMAÇÃO NUTRICIONAL---\n"
    textData.append(infoDescription)
    textData.append("------------------------------\n")
    return textData.toString()
  }

  fun createReceiptData(product: MgvProduct): Boolean {

    try {

      Timber.i("Produto: $product")

      printer?.addTextAlign(Printer.ALIGN_CENTER)
      printer?.addFeedLine(1)

      val description = createDescription(product.description)
      printer?.addText(description)
      printer?.addFeedLine(2)

      val weightPerKg = createWeight(product.weight!!)
      printer?.addText(weightPerKg)

      val pricePerKg = createPricePerKg(product.price)
      printer?.addText(pricePerKg)
      printer?.addFeedLine(3)

      val totalPrice = createTotalPrice(product.price, product.weight!!)
      formatBoldText()
      printer?.addText(totalPrice)
      printer?.addFeedLine(2)

      if (product.nutrictionFacts != null) {
        val titleNutriction = createDescription()
        printer?.addText(titleNutriction)
        val infoNutritional = createNutriction(product.nutrictionFacts!!)
        formatDefaultText()
        printer?.addText(infoNutritional)
        printer?.addFeedLine(2)
      }

      val barcode = createBarcode(product.code, product.weight!!)
      val barcodeWidth = 3
      val barcodeHeight = 100

      printer?.addBarcode(
        barcode,
        Printer.BARCODE_EAN13,
        Printer.HRI_BELOW,
        Printer.FONT_A,
        barcodeWidth,
        barcodeHeight
      )

      printer?.addFeedLine(5)

      
      printer?.addCut(Printer.CUT_FEED)

    } catch (e: Exception) {
      e.printStackTrace()
      printer?.clearCommandBuffer()
      return false
    }

    return true
  }

  fun requestPermission(device: UsbDevice, context: Context) {
    val permissionIntent = PendingIntent.getBroadcast(
      context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
    )
    usbManager.requestPermission(device, permissionIntent)
  }

  fun discoveryPrinter() {
    val filterOptions = FilterOption()
    filterOptions.deviceType = Discovery.TYPE_PRINTER
    filterOptions.epsonFilter = Discovery.FILTER_NAME
    filterOptions.usbDeviceName = Discovery.TRUE
    Discovery.start(context, filterOptions, this)
  }

  private fun connectPrinter(): Boolean {

    if (printer == null) {
      Timber.i("Impressora não definida")
      return false
    }
    try {
      printer?.connect("USB:", Printer.PARAM_DEFAULT)
      return true
    } catch (e: Exception) {
      throw e
    }
  }


  fun printData(): Boolean {

    Timber.i("Realizando impressão USB")

    if (!connectPrinter()) {
      Timber.i("Falha na conexão usb")
      printer?.clearCommandBuffer()
      return false
    }

    try {
      Timber.i("Enviando dados")
      printer?.sendData(Printer.PARAM_DEFAULT)
    } catch (e: Exception) {
      printer?.clearCommandBuffer()
      e.printStackTrace()
      try {
        printer?.disconnect()
      } catch (ex: Exception) {
        ex.printStackTrace()
        // Do nothing
      }
      return false
    }
    return true
  }

  private fun getPrinterModel(name: String): Int {
    return when (name) {
      "TM-L100" -> Printer.TM_L100
      else -> 0
    }
  }

  override fun onDiscovery(deviceInfo: DeviceInfo) {
    printerName = deviceInfo.deviceName
    printerPort = deviceInfo.target
  }

  override fun onPtrReceive(printer: Printer?, p1: Int, status: PrinterStatusInfo?, p3: String?) {
    disconnect()
  }

  private fun disconnect() {
    if (printer == null) {
      return
    }
    try {
      printer?.disconnect()
      printer?.clearCommandBuffer()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}