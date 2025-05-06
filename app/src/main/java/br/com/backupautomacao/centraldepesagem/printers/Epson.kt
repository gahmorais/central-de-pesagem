package br.com.backupautomacao.centraldepesagem.printers

import android.content.Context
import com.epson.epos2.discovery.DeviceInfo
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epos2.printer.Printer

class Epson : DiscoveryListener {
  private lateinit var printer: Printer
  private lateinit var printerPort: String
  private lateinit var printerName: String
  fun initializePrinter() {

  }

  private fun createReceiptData(): Boolean {
    var method: String
//    val logoData = BitmapFactory.decodeResource(getResources(), R.drawable.store)
    val textData = StringBuilder()
    val barcodeWidth = 2
    val barcodeHeight = 100

    try {

      method = "addTextAlign"
      printer.addTextAlign(Printer.ALIGN_CENTER)

      method = "addFeedLine"
      printer.addFeedLine(1)
      textData.append("THE STORE 123 (555) 555 – 5555\n")
      textData.append("STORE DIRECTOR – John Smith\n")
      textData.append("\n")
      textData.append("7/01/07 16:58 6153 05 0191 134\n")
      textData.append("ST# 21 OP# 001 TE# 01 TR# 747\n")
      textData.append("------------------------------\n")
      method = "addText"
      printer.addText(textData.toString())
      textData.delete(0, textData.length)

      textData.append("400 OHEIDA 3PK SPRINGF  9.99 R\n")
      textData.append("410 3 CUP BLK TEAPOT    9.99 R\n")
      textData.append("445 EMERIL GRIDDLE/PAN 17.99 R\n")
      textData.append("438 CANDYMAKER ASSORT   4.99 R\n")
      textData.append("474 TRIPOD              8.99 R\n")
      textData.append("433 BLK LOGO PRNTED ZO  7.99 R\n")
      textData.append("458 AQUA MICROTERRY SC  6.99 R\n")
      textData.append("493 30L BLK FF DRESS   16.99 R\n")
      textData.append("407 LEVITATING DESKTOP  7.99 R\n")
      textData.append("441 **Blue Overprint P  2.99 R\n")
      textData.append("476 REPOSE 4PCPM CHOC   5.49 R\n")
      textData.append("461 WESTGATE BLACK 25  59.99 R\n")
      textData.append("------------------------------\n")
      method = "addText"
      printer.addText(textData.toString())
      textData.delete(0, textData.length)

      textData.append("SUBTOTAL                160.38\n")
      textData.append("TAX                      14.43\n")
      method = "addText"
      printer.addText(textData.toString())
      textData.delete(0, textData.length)

      method = "addTextSize"
      printer.addTextSize(2, 2)
      method = "addText"
      printer.addText("TOTAL    174.81\n")
      method = "addTextSize"
      printer.addTextSize(1, 1)
      method = "addFeedLine"
      printer.addFeedLine(1)

      textData.append("CASH                    200.00\n")
      textData.append("CHANGE                   25.19\n")
      textData.append("------------------------------\n")
      method = "addText"
      printer.addText(textData.toString())
      textData.delete(0, textData.length)

      textData.append("Purchased item total number\n")
      textData.append("Sign Up and Save !\n")
      textData.append("With Preferred Saving Card\n")
      method = "addText"
      printer.addText(textData.toString())
      textData.delete(0, textData.length)
      method = "addFeedLine"
      printer.addFeedLine(2)

      method = "addBarcode"
      printer.addBarcode(
        "01209457",
        Printer.BARCODE_CODE39,
        Printer.HRI_BELOW,
        Printer.FONT_A,
        barcodeWidth,
        barcodeHeight
      )

      method = "addCut"
      printer.addCut(Printer.CUT_FEED)
    } catch (e: Exception) {
      e.printStackTrace()
      printer.clearCommandBuffer()

      return false
    }

    return true
  }

  fun discoveryPrinter(context: Context) {
    val filterOptions = FilterOption()
    filterOptions.deviceType = Discovery.TYPE_PRINTER
    filterOptions.epsonFilter = Discovery.FILTER_NAME
    filterOptions.usbDeviceName = Discovery.TRUE
    Discovery.start(context, filterOptions, this)
  }

  private fun connectPrinter(): Boolean {

    try {
      printer.connect(printerPort, Printer.PARAM_DEFAULT)
    } catch (e: Exception) {
      return false
    }

    return true
  }

  private fun printData(): Boolean {

    if (!connectPrinter()) {
      printer.clearCommandBuffer()
      return false
    }

    try {
      printer.sendData(Printer.PARAM_DEFAULT)
    } catch (e: Exception) {
      printer.clearCommandBuffer()

      try {
        printer.disconnect()
      } catch (ex: Exception) {
        // Do nothing
      }
      return false
    }
    return true
  }

  override fun onDiscovery(deviceInfo: DeviceInfo) {
    printerName = deviceInfo.deviceName
    printerPort = deviceInfo.target
  }
}