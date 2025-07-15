package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseConservacaoInfo (
    @SerializedName("ConservadorExtras")
    val conservadorInfos: List<ConservadorInfo>?,
    @SerializedName("ExportaInformacaoExtraResult")
    val mgvError: MgvError?
)
data class ConservadorInfo(
    @SerializedName("codigo")
    val code: String,
    @SerializedName("observacao")
    val observacao: String,
    @SerializedName("linha_1")
    val conservacao: String,
    @SerializedName("linha_2")
    val conservacao2: String,
    @SerializedName("linha_3")
    val conservacao3: String,
    @SerializedName("linha_4")
    val conservacao4: String,
    @SerializedName("linha_5")
    val conservacao5: String,
    @SerializedName("linha_6")
    val conservacao6: String,
    @SerializedName("linha_7")
    val conservacao7: String,
    @SerializedName("linha_8")
    val conservacao8: String,
    @SerializedName("linha_9")
    val conservacao9: String,
    @SerializedName("linha_10")
    val conservacao10: String,
)