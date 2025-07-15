package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseFracionadorInfo (
    @SerializedName("FracionadorExtras")
    val fracionadorInfos: List<FracionadorInfo>?,
    @SerializedName("ExportaInformacaoExtraResult")
    val mgvError: MgvError?
)

data class FracionadorInfo(
    @SerializedName("codigo")
    val code: String,
    @SerializedName("observacao")
    val observacao: String,
    @SerializedName("linha_1")
    val fracionador: String,
    @SerializedName("linha_2")
    val fracionador2: String,
    @SerializedName("linha_3")
    val fracionador3: String,
    @SerializedName("linha_4")
    val fracionador4: String,
    @SerializedName("linha_5")
    val fracionador5: String
)