package br.com.backupautomacao.centraldepesagem.utils


import br.com.backupautomacao.centraldepesagem.FileController
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatBarcode
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatPrice
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatWeight
import java.text.SimpleDateFormat
import java.util.*

class Label {
  fun createSimpleLabel(mgvProduct: MgvProduct): String {
    val fileController = FileController()
    val label: String = fileController.readFile("etiqueta_modelo_simples.prn")
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    val now = System.currentTimeMillis()
    val currentDateFormatted = formatter.format(now)

    val ONE_DAY_MILLIS = 60 * 60 * 24 * 1000
    val dateForward = now + (ONE_DAY_MILLIS * mgvProduct.DiasValidade)
    val dateForwardFormatted = formatter.format(dateForward)
    val weightFormatted = formatWeight(mgvProduct.weight)

    val totalPrice = mgvProduct.Preco.times(mgvProduct.weight!!)
    val totaPriceFormatted = formatPrice(totalPrice)
    val pricePerKilogramFormatted = formatPrice(mgvProduct.Preco)

    return label
      .replace("var_descricao", mgvProduct.Descritivo1aLinha)
      .replace("var_codigo", formatBarcode(mgvProduct.Codigo, mgvProduct.Preco))
      .replace("var_data_embalado", currentDateFormatted)
      .replace("var_data_validade", dateForwardFormatted)
      .replace("var_peso", weightFormatted)
      .replace("var_preco_kilo", pricePerKilogramFormatted)
      .replace("var_preco", totaPriceFormatted)
  }
}