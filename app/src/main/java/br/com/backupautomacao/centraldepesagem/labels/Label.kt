package br.com.backupautomacao.centraldepesagem.labels

import android.content.Context
import br.com.backupautomacao.centraldepesagem.Constants
import br.com.backupautomacao.centraldepesagem.FileController
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*

interface Label {
  val context: Context
  fun generateLabel(mgvProduct: MgvProduct): String {
    val fileController = FileController(context)
    var labelBase: String = try {
      fileController.readFileFromAssets("labels/ET119.zpl")
    } catch (e: FileNotFoundException) {
      throw e
    }

    val label: StringBuilder = StringBuilder()
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    val now = System.currentTimeMillis()
    val currentDateFormatted = formatter.format(now)

    val dateForward = now + (Constants.ONE_DAY_MILLIS * mgvProduct.shelfLife)
    val dateForwardFormatted = formatter.format(dateForward)
    val weightFormatted = Formatter.formatWeight(mgvProduct.weight)

    val totalPrice = mgvProduct.price.times(mgvProduct.weight!!)
    val totalPriceFormatted = Formatter.formatPrice(totalPrice)
    val pricePerKilogramFormatted = Formatter.formatPrice(mgvProduct.price)

    labelBase = labelBase
      .replace("var_descricao", mgvProduct.description)
      .replace("var_codigo", Formatter.formatBarcode(mgvProduct.code, totalPrice))
      .replace("var_data_embalado", currentDateFormatted)
      .replace("var_data_validade", dateForwardFormatted)
      .replace("var_peso", weightFormatted)
      .replace("var_preco_kilo", pricePerKilogramFormatted)
      .replace("var_preco", totalPriceFormatted)
      .replace("var_tara", Formatter.formatWeight(mgvProduct.codTara))

    label.append(labelBase)
    label.append("^PQ1,0,1,Y^XZ")

    return String(label)
  }
}