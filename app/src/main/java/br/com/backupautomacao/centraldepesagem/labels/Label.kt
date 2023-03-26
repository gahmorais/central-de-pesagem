package br.com.backupautomacao.centraldepesagem.labels

import android.content.Context
import android.util.Log
import br.com.backupautomacao.centraldepesagem.Constants
import br.com.backupautomacao.centraldepesagem.FileController
import br.com.backupautomacao.centraldepesagem.models.MgvProductImpl
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*

interface Label {
  val context: Context
  val name: String
  fun generateLabel(mgvProduct: MgvProductImpl): String {
    val fileController = FileController(context)
    var labelBase: String = try {
      fileController.readFileFromAssets("labels/${name}.zpl")
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
    Log.i("Label", "generateLabel: $mgvProduct")

    val nutrictionFacts = mgvProduct.nutrictionFacts
    val extraInfo = mgvProduct.extraInfo
    labelBase = labelBase
      .replace("var_descricao", mgvProduct.description)
      .replace("var_codigo", Formatter.formatBarcode(mgvProduct.code, totalPrice))
      .replace("var_data_embalado", currentDateFormatted)
      .replace("var_data_validade", dateForwardFormatted)
      .replace("var_peso", weightFormatted)
      .replace("var_preco_kilo", pricePerKilogramFormatted)
      .replace("var_preco", totalPriceFormatted)
      .replace("var_tara", Formatter.formatWeight(mgvProduct.codTara))
      .replace("var_primeira_linha", extraInfo?.line1 ?: "")
      .replace("var_segunda_linha", extraInfo?.line2 ?: "")
      .replace("var_terceira_linha", extraInfo?.line3 ?: "")
      .replace("var_quarta_linha", extraInfo?.line4 ?: "")
      .replace("var_quinta_linha", extraInfo?.line5 ?: "")
      .replace("var_valor_energetico", Formatter.formatNutriction(nutrictionFacts?.ValorEnergetico))
      .replace("var_carboidratos", Formatter.formatNutriction(nutrictionFacts?.Carboidrato))
      .replace("var_proteinas", Formatter.formatNutriction(nutrictionFacts?.Proteina))
      .replace("var_gord_totais", Formatter.formatNutriction(nutrictionFacts?.GorduraTotal))
      .replace("var_gord_saturadas", Formatter.formatNutriction(nutrictionFacts?.GorduraSaturada))
      .replace("var_gorduras_trans", Formatter.formatNutriction(nutrictionFacts?.GorduraTrans))
      .replace("var_fibra_alimentar", Formatter.formatNutriction(nutrictionFacts?.FibraAlimentar))
      .replace("var_sodio", Formatter.formatNutriction(nutrictionFacts?.Sodio))
      .replace("vd_energetico", "0%")
      .replace("vd_carboidratos", "0%")
      .replace("vd_proteinas", "0%")
      .replace("vd_gord_totais", "0%")
      .replace("vd_gord_saturadas", "0%")
      .replace("vd_gord_trans", "0%")
      .replace("vd_fibra_alimentar", "0%")
      .replace("vd_sodio", "0%")

    label.append(labelBase)
    label.append("^PQ1,0,1,Y^XZ")

    return String(label)
  }

}