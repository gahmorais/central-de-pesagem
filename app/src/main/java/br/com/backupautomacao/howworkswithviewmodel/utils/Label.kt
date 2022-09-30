package br.com.backupautomacao.howworkswithviewmodel.utils


import br.com.backupautomacao.howworkswithviewmodel.FileController
import br.com.backupautomacao.howworkswithviewmodel.network.MgvProduct
import br.com.backupautomacao.howworkswithviewmodel.utils.Formatter.Companion.formatBarcode
import br.com.backupautomacao.howworkswithviewmodel.utils.Formatter.Companion.formatPrice
import br.com.backupautomacao.howworkswithviewmodel.utils.Formatter.Companion.formatWeight
import java.text.SimpleDateFormat
import java.util.*

class Label(
  private val price: Double = 0.0,
  private val pricePerKilograms: Double = 0.0,
  private val description: String = "",
  private val codeItem: String = "",
  private val weight: Double = 0.0
) {

  fun gerarEtiquetaSimples(mgvProduct: MgvProduct): String {
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