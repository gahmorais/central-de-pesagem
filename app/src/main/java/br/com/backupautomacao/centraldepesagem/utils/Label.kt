package br.com.backupautomacao.centraldepesagem.utils

import android.content.Context
import br.com.backupautomacao.centraldepesagem.FileController
import br.com.backupautomacao.centraldepesagem.network.ExtraInfo
import br.com.backupautomacao.centraldepesagem.network.MgvInformacoesNutricionais
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatBarcode
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatNutriction
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatPrice
import br.com.backupautomacao.centraldepesagem.utils.Formatter.Companion.formatWeight
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*

private const val ONE_DAY_MILLIS = 60 * 60 * 24 * 1000

class Label(context: Context) {

    private val fileController = FileController(context)
    private val BASE_LENGTH = "200"
    private val NUTRICTION_LENGTH = "560"
    private val FULL_LENGTH = "800"
    fun generateLabelBase(mgvProduct: MgvProduct, labelName: String?): String {

        var labelBase: String = try {
            fileController.readFileFromAssets(labelName ?: "etiqueta_modelo_grande.prn")
        } catch (e: FileNotFoundException) {
            throw e
        }
        val label: StringBuilder = StringBuilder()
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val (nutrictionFacts, extraInfo) = mgvProduct
        val now = System.currentTimeMillis()
        val currentDateFormatted = formatter.format(now)

        val dateForward = now + (ONE_DAY_MILLIS * mgvProduct.shelfLife)
        val dateForwardFormatted = formatter.format(dateForward)
        val weightFormatted = formatWeight(mgvProduct.weight)

        val totalPrice = mgvProduct.price.times(mgvProduct.weight!!)
        val totalPriceFormatted = formatPrice(totalPrice)
        val pricePerKilogramFormatted = formatPrice(mgvProduct.price)

        labelBase = labelBase
            .replace("var_descricao", mgvProduct.description)
            .replace("var_codigo", formatBarcode(mgvProduct.code, totalPrice))
            .replace("var_data_embalado", currentDateFormatted)
            .replace("var_data_validade", dateForwardFormatted)
            .replace("var_peso", weightFormatted)
            .replace("var_preco_kilo", pricePerKilogramFormatted)
            .replace("var_preco", totalPriceFormatted)
            .replace("var_tara", formatWeight(mgvProduct.codTara))
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

        var label: String = try {
            fileController.readFileFromAssets("extra_info.txt")
        } catch (e: FileNotFoundException) {
            throw e
        }

        label = label
            .replace("var_info_1", extraInfo.line1)
            .replace("var_info_2", extraInfo.line2)
            .replace("var_info_3", extraInfo.line3)
            .replace("var_info_4", extraInfo.line4)
            .replace("var_info_5", extraInfo.line5)
            .replace("var_info_6", extraInfo.line6)
            .replace("var_info_7", extraInfo.line7)
            .replace("var_info_8", extraInfo.line8)
            .replace("var_info_9", extraInfo.line9)
            .replace("var_info_10", extraInfo.line10)
            .replace("var_info_11", extraInfo.line11)
            .replace("var_info_12", extraInfo.line12)
            .replace("var_info_13", extraInfo.line13)
            .replace("var_info_14", extraInfo.line14)
            .replace("var_info_15", extraInfo.line15)
            .replace("var_info_16", extraInfo.line16)
            .replace("var_info_17", extraInfo.line17)
            .replace("var_info_18", extraInfo.line18)
            .replace("var_info_19", extraInfo.line19)
            .replace("var_info_20", extraInfo.line20)

        labelBase.append(label)
    }

    private fun appendNutrictionFacts(
        labelBase: StringBuilder,
        nutrictionFacts: MgvInformacoesNutricionais
    ) {

        var label: String = try {
            fileController.readFileFromAssets("nutri_info.txt")
        } catch (e: FileNotFoundException) {
            throw e
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

    fun generateLabelWithQuantity(
        mgvProduct: MgvProduct,
        labelName: String?,
        quantity: Int
    ): String {
        return generateLabelBase(mgvProduct, labelName)
            .replace("var_quantidade", quantity.toString())

    }
}