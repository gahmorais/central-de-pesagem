package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseExtraInfo(
    @SerializedName("informacoesExtras")
    val extraInfos: List<ExtraInfo>?,
    @SerializedName("ExportaInformacaoExtraResult")
    val mgvError: MgvError?
)

data class ExtraInfo(
    @SerializedName("Codigo")
    val code: String,
    @SerializedName("Observacao")
    val observacao: String,
    @SerializedName("Linha1")
    val line1: String,
    @SerializedName("Linha2")
    val line2: String,
    @SerializedName("Linha3")
    val line3: String,
    @SerializedName("Linha4")
    val line4: String,
    @SerializedName("Linha5")
    val line5: String,
    @SerializedName("Linha6")
    val line6: String,
    @SerializedName("Linha7")
    val line7: String,
    @SerializedName("Linha8")
    val line8: String,
    @SerializedName("Linha9")
    val line9: String,
    @SerializedName("Linha10")
    val line10: String,
    @SerializedName("Linha11")
    val line11: String,
    @SerializedName("Linha12")
    val line12: String,
    @SerializedName("Linha13")
    val line13: String,
    @SerializedName("Linha14")
    val line14: String,
    @SerializedName("Linha15")
    val line15: String,
    @SerializedName("Linha16")
    val line16: String,
    @SerializedName("Linha17")
    val line17: String,
    @SerializedName("Linha18")
    val line18: String,
    @SerializedName("Linha19")
    val line19: String,
    @SerializedName("Linha20")
    val line20: String,
    @SerializedName("CampoExtra1")
    val campoextra: String,
    @SerializedName("CampoExtra2")
    val campoextra2: String,
    @SerializedName("CampoExtra3")
    val campoextra3: String,
)