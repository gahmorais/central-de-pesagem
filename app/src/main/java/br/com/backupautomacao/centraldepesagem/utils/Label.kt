package br.com.backupautomacao.centraldepesagem.utils

import br.com.backupautomacao.centraldepesagem.FileController
import br.com.backupautomacao.centraldepesagem.network.ExtraInfo
import br.com.backupautomacao.centraldepesagem.network.MgvInformacoesNutricionais
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatBarcode
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatNutriction
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatPrice
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatWeight
import java.text.SimpleDateFormat
import java.util.*

private const val ONE_DAY_MILLIS = 60 * 60 * 24 * 1000

class Label(
  private val fileController: FileController = FileController()
) {
  private val BASE_LENGTH = "200"
  private val NUTRICTION_LENGTH = "560"
  private val FULL_LENGTH = "800"
  fun createLabelBase(mgvProduct: MgvProduct): String {
//    var labelBase: String = fileController.readFile("base_etq.txt")
    var labelBase: String = fileController.readFileFromDownloadFolder("etiqueta-sato.prn")
    val label: StringBuilder = StringBuilder()
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    val (nutrictionFacts, extraInfo) = mgvProduct
    val now = System.currentTimeMillis()
    val currentDateFormatted = formatter.format(now)

    val dateForward = now + (ONE_DAY_MILLIS * mgvProduct.DiasValidade)
    val dateForwardFormatted = formatter.format(dateForward)
    val weightFormatted = formatWeight(mgvProduct.weight)

    val totalPrice = mgvProduct.Preco.times(mgvProduct.weight!!)
    val totalPriceFormatted = formatPrice(totalPrice)
    val pricePerKilogramFormatted = formatPrice(mgvProduct.Preco)
    labelBase = labelBase
      .replace("var_descricao", mgvProduct.Descritivo1aLinha)
      .replace("var_codigo", formatBarcode(mgvProduct.Codigo, totalPrice))
      .replace("var_data_embalado", currentDateFormatted)
      .replace("var_data_validade", dateForwardFormatted)
      .replace("var_peso", weightFormatted)
      .replace("var_preco_kilo", pricePerKilogramFormatted)
      .replace("var_preco", totalPriceFormatted)
      .replace("var_tara", formatWeight(mgvProduct.CodTara))
    label.append(labelBase)
    var length = BASE_LENGTH

    nutrictionFacts?.let {
      appendNutrictionFacts(label, mgvProduct.nutrictionFacts!!)
      length = NUTRICTION_LENGTH
    }

    extraInfo?.let {
      appendExtraInfo(label, mgvProduct.extraInfo!!)
      length = FULL_LENGTH
    }

    label.append("^PQ1,0,1,Y^XZ")

    return String(label).replace("var_comprimento", length)
  }

  private fun appendExtraInfo(labelBase: StringBuilder, extraInfo: ExtraInfo) {
    var label: String = fileController.readFileFromDownloadFolder("extra_info.txt")
    label = label
      .replace("var_info_3", extraInfo.Linha3)
      .replace("var_info_4", extraInfo.Linha4)
      .replace("var_info_5", extraInfo.Linha5)
      .replace("var_info_6", extraInfo.Linha6)
      .replace("var_info_7", extraInfo.Linha7)
      .replace("var_info_8", extraInfo.Linha8)
      .replace("var_info_9", extraInfo.Linha9)
      .replace("var_info_10", extraInfo.Linha10)
      .replace("var_info_11", extraInfo.Linha11)
      .replace("var_info_12", extraInfo.Linha12)
      .replace("var_info_13", extraInfo.Linha13)
      .replace("var_info_14", extraInfo.Linha14)
      .replace("var_info_15", extraInfo.Linha15)
      .replace("var_info_16", extraInfo.Linha16)
      .replace("var_info_17", extraInfo.Linha17)
      .replace("var_info_18", extraInfo.Linha18)
      .replace("var_info_19", extraInfo.Linha19)
      .replace("var_info_20", extraInfo.Linha20)
      .replace("var_info_1", extraInfo.Linha1)
      .replace("var_info_2", extraInfo.Linha2)
    labelBase.append(label)
  }

  private fun appendNutrictionFacts(
    labelBase: StringBuilder,
    nutrictionFacts: MgvInformacoesNutricionais
  ) {
    val fileController = FileController()
    var label: String = fileController.readFileFromDownloadFolder("nutri_info.txt")
    val randomNumbers = (0..21).map { Util.generateRandomDouble() }
    val randomNumberInt = (0..2).map { Util.generateRandomInt() }
    nutrictionFacts.ValorEnergetico?.let {
      label = label
        .replace("ve_100", formatNutriction(nutrictionFacts.ValorEnergetico))
        .replace("ve_000", formatNutriction(randomNumberInt[0]))
        .replace("ve_vd", formatNutriction(randomNumberInt[1]))
    }
    nutrictionFacts.Carboidrato?.let {
      label = label
        .replace("ct_100", formatNutriction(nutrictionFacts.Carboidrato))
        .replace("ct_000", formatNutriction(randomNumbers[2]))
        .replace("ct_vd", formatNutriction(randomNumbers[3]))
    }
    nutrictionFacts.Proteina?.let {
      label = label
        .replace("prot_100", formatNutriction(nutrictionFacts.Proteina))
        .replace("prot_000", formatNutriction(randomNumbers[4]))
        .replace("prot_vd", formatNutriction(randomNumbers[5]))
    }

    nutrictionFacts.GorduraTotal?.let {
      label = label
        .replace("gt_100", formatNutriction(nutrictionFacts.GorduraTotal))
        .replace("gt_000", formatNutriction(randomNumbers[6]))
        .replace("gt_vd", formatNutriction(randomNumbers[7]))
    }

    nutrictionFacts.GorduraSaturada?.let {
      label = label
        .replace("gs_100", formatNutriction(nutrictionFacts.GorduraSaturada))
        .replace("gs_000", formatNutriction(randomNumbers[8]))
        .replace("gs_vd", formatNutriction(randomNumbers[9]))
    }

    nutrictionFacts.GorduraTrans?.let {
      label = label
        .replace("gtr_100", formatNutriction(nutrictionFacts.GorduraTrans))
        .replace("gtr_000", formatNutriction(randomNumbers[10]))
        .replace("gtr_vd", formatNutriction(randomNumbers[11]))
    }

    nutrictionFacts.FibraAlimentar?.let {
      label = label
        .replace("fa_100", formatNutriction(nutrictionFacts.FibraAlimentar))
        .replace("fa_000", formatNutriction(randomNumbers[12]))
        .replace("fa_vd", formatNutriction(randomNumbers[13]))
    }

    nutrictionFacts.Sodio?.let {
      label = label
        .replace("so_100", formatNutriction(nutrictionFacts.Sodio))
        .replace("so_000", formatNutriction(randomNumbers[14]))
        .replace("so_vd", formatNutriction(randomNumbers[15]))
    }
    label = label
      .replace("var_comprimento", NUTRICTION_LENGTH)
      .replace("porcao_emb", randomNumberInt[1].toString())
      .replace("med_caseira", formatNutriction(nutrictionFacts.MedidaCaseira))
      .replace("at_100", formatNutriction(randomNumbers[16]))
      .replace("at_000", formatNutriction(randomNumbers[17]))
      .replace("at_vd", formatNutriction(randomNumbers[18]))
      .replace("ac_add_100", formatNutriction(randomNumbers[19]))
      .replace("ac_add_000", formatNutriction(randomNumbers[20]))
      .replace("ac_add_vd", formatNutriction(randomNumbers[21]))

    labelBase.append(label)
  }

  fun createLabelRdc429(mgvProduct: MgvProduct): String {
    var label: String = fileController.readFileFromDownloadFolder("etiqueta_rdc429.prn")
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    val (nutrictionFacts) = mgvProduct
    val now = System.currentTimeMillis()
    val currentDateFormatted = formatter.format(now)

    val dateForward = now + (ONE_DAY_MILLIS * mgvProduct.DiasValidade)
    val dateForwardFormatted = formatter.format(dateForward)
    val weightFormatted = formatWeight(mgvProduct.weight)

    val totalPrice = mgvProduct.Preco.times(mgvProduct.weight!!)
    val totalPriceFormatted = formatPrice(totalPrice)
    val pricePerKilogramFormatted = formatPrice(mgvProduct.Preco)

    label = label
      .replace("var_descricao", mgvProduct.Descritivo1aLinha)
      .replace("var_codigo", formatBarcode(mgvProduct.Codigo, mgvProduct.Preco))
      .replace("var_data_embalado", currentDateFormatted)
      .replace("var_data_validade", dateForwardFormatted)
      .replace("var_peso", weightFormatted)
      .replace("var_preco_kilo", pricePerKilogramFormatted)
      .replace("var_preco", totalPriceFormatted)
      .replace("var_tara", formatWeight(mgvProduct.CodTara))

    if (nutrictionFacts == null) {
      return label
    }

    val randomNumbers = (0..21).map { Util.generateRandomDouble() }
    val randomNumberInt = (0..2).map { Util.generateRandomInt() }
    nutrictionFacts.ValorEnergetico?.let {
      label = label
        .replace("ve_100", formatNutriction(nutrictionFacts.ValorEnergetico))
        .replace("ve_000", formatNutriction(randomNumberInt[0]))
        .replace("ve_vd", formatNutriction(randomNumberInt[1]))
    }
    nutrictionFacts.Carboidrato?.let {
      label = label
        .replace("ct_100", formatNutriction(nutrictionFacts.Carboidrato))
        .replace("ct_000", formatNutriction(randomNumbers[2]))
        .replace("ct_vd", formatNutriction(randomNumbers[3]))
    }
    nutrictionFacts.Proteina?.let {
      label = label
        .replace("prot_100", formatNutriction(nutrictionFacts.Proteina))
        .replace("prot_000", formatNutriction(randomNumbers[4]))
        .replace("prot_vd", formatNutriction(randomNumbers[5]))
    }

    nutrictionFacts.GorduraTotal?.let {
      label = label
        .replace("gt_100", formatNutriction(nutrictionFacts.GorduraTotal))
        .replace("gt_000", formatNutriction(randomNumbers[6]))
        .replace("gt_vd", formatNutriction(randomNumbers[7]))
    }

    nutrictionFacts.GorduraSaturada?.let {
      label = label
        .replace("gs_100", formatNutriction(nutrictionFacts.GorduraSaturada))
        .replace("gs_000", formatNutriction(randomNumbers[8]))
        .replace("gs_vd", formatNutriction(randomNumbers[9]))
    }

    nutrictionFacts.GorduraTrans?.let {
      label = label
        .replace("gtr_100", formatNutriction(nutrictionFacts.GorduraTrans))
        .replace("gtr_000", formatNutriction(randomNumbers[10]))
        .replace("gtr_vd", formatNutriction(randomNumbers[11]))
    }

    nutrictionFacts.FibraAlimentar?.let {
      label = label
        .replace("fa_100", formatNutriction(nutrictionFacts.FibraAlimentar))
        .replace("fa_000", formatNutriction(randomNumbers[12]))
        .replace("fa_vd", formatNutriction(randomNumbers[13]))
    }

    nutrictionFacts.Sodio?.let {
      label = label
        .replace("so_100", formatNutriction(nutrictionFacts.Sodio))
        .replace("so_000", formatNutriction(randomNumbers[14]))
        .replace("so_vd", formatNutriction(randomNumbers[15]))
    }

    return label
      .replace("porcao_emb", randomNumberInt[1].toString())
      .replace("med_caseira", formatNutriction(nutrictionFacts.MedidaCaseira))
      .replace("at_100", formatNutriction(randomNumbers[16]))
      .replace("at_000", formatNutriction(randomNumbers[17]))
      .replace("at_vd", formatNutriction(randomNumbers[18]))
      .replace("ac_add_100", formatNutriction(randomNumbers[19]))
      .replace("ac_add_000", formatNutriction(randomNumbers[20]))
      .replace("ac_add_vd", formatNutriction(randomNumbers[21]))
  }

  fun createSimpleLabel(mgvProduct: MgvProduct): String {
    val label: String = fileController.readFileFromDownloadFolder("etiqueta_modelo_simples.prn")
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    val now = System.currentTimeMillis()
    val currentDateFormatted = formatter.format(now)

    val dateForward = now + (ONE_DAY_MILLIS * mgvProduct.DiasValidade)
    val dateForwardFormatted = formatter.format(dateForward)
    val weightFormatted = formatWeight(mgvProduct.weight)

    val totalPrice = mgvProduct.Preco.times(mgvProduct.weight!!)
    val totalPriceFormatted = formatPrice(totalPrice)
    val pricePerKilogramFormatted = formatPrice(mgvProduct.Preco)
    return label
      .replace("var_descricao", mgvProduct.Descritivo1aLinha)
      .replace("var_codigo", formatBarcode(mgvProduct.Codigo, mgvProduct.Preco))
      .replace("var_data_embalado", currentDateFormatted)
      .replace("var_data_validade", dateForwardFormatted)
      .replace("var_peso", weightFormatted)
      .replace("var_preco_kilo", pricePerKilogramFormatted)
      .replace("var_preco", totalPriceFormatted)
  }
}