package br.com.backupautomacao.pesagem_modular.network

import com.google.gson.annotations.SerializedName

data class MgvError(
    @SerializedName("Codigo")
    val code: String,
    @SerializedName("CodigoDaLoja")
    val branchNumber: Int,
    @SerializedName("Msg")
    val message: String
)