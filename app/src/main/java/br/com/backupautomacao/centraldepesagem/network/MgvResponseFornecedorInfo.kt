package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseFornecedorInfo (
    @SerializedName("FornecedorExtras")
    val fornecedorInfos: List<FornecedorInfo>?,
    @SerializedName("ExportaInformacaoExtraResult")
    val mgvError: MgvError?
)
data class FornecedorInfo(
    @SerializedName("codigo")
    val code: String,
    @SerializedName("observacao")
    val observacao: String,
    @SerializedName("linha_1")
    val fornecedor: String,
    @SerializedName("linha_2")
    val fornecedor2: String,
    @SerializedName("linha_3")
    val fornecedor3: String,
    @SerializedName("linha_4")
    val fornecedor4: String,
    @SerializedName("linha_5")
    val fornecedor5: String
)