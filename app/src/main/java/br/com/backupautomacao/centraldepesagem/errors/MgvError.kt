package br.com.backupautomacao.centraldepesagem.errors

import com.google.gson.annotations.SerializedName

const val LOJA_NAO_ENCONTRADA = "-3"
const val PRODUTO_NAO_ENCONTRADO = "-6"
data class MgvError(
  @SerializedName("Codigo")
  val code: String,
  @SerializedName("CodigoDaLoja")
  val branchNumber: Int,
  @SerializedName("Msg")
  val message: String
)