package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseExtraInfo(
    @SerializedName("informacao_extra")
    val extraInfos: List<ExtraInfo>?,
    @SerializedName("ExportaInformacaoExtraResult")
    val mgvError: MgvError?
)

data class ExtraInfo(
    @SerializedName("Codigo")
    val code: String,
    @SerializedName("Observacao")
    val observacao: String,
    @SerializedName("linha__1")
    val line1: String,
    @SerializedName("linha_2")
    val line2: String,
    @SerializedName("linha_3")
    val line3: String,
    @SerializedName("linha_4")
    val line4: String,
    @SerializedName("linha_5")
    val line5: String,
    @SerializedName("linha_6")
    val line6: String,
    @SerializedName("linha_7")
    val line7: String,
    @SerializedName("linha_8")
    val line8: String,
    @SerializedName("linha_9")
    val line9: String,
    @SerializedName("linha_10")
    val line10: String,
    @SerializedName("linha_11")
    val line11: String,
    @SerializedName("linha_12")
    val line12: String,
    @SerializedName("linha_13")
    val line13: String,
    @SerializedName("linha_14")
    val line14: String,
    @SerializedName("linha_15")
    val line15: String,
    @SerializedName("linha_16")
    val line16: String,
    @SerializedName("linha_17")
    val line17: String,
    @SerializedName("linha_18")
    val line18: String,
    @SerializedName("linha_19")
    val line19: String,
    @SerializedName("linha_20")
    val line20: String,
    @SerializedName("CampoExtra1")
    val campoextra: String,
    @SerializedName("CampoExtra2")
    val campoextra2: String,
    @SerializedName("CampoExtra3")
    val campoextra3: String,
)