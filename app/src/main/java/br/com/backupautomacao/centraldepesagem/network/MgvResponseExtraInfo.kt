package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseExtraInfo(
  @SerializedName("informacoesExtras")
  val extraInfos: List<ExtraInfo>?,
  @SerializedName("ExportaInformacaoExtraResult")
  val extraInfoError: MgvError?
)

data class ExtraInfo(
  @SerializedName("Codigo")
  val code: String,
  @SerializedName("Linha1")
  val Linha1: String,
  @SerializedName("Linha2")
  val Linha2: String,
  @SerializedName("Linha3")
  val Linha3: String,
  @SerializedName("Linha4")
  val Linha4: String,
  @SerializedName("Linha5")
  val Linha5: String,
  @SerializedName("Linha6")
  val Linha6: String,
  @SerializedName("Linha7")
  val Linha7: String,
  @SerializedName("Linha8")
  val Linha8: String,
  @SerializedName("Linha9")
  val Linha9: String,
  @SerializedName("Linha10")
  val Linha10: String,
  @SerializedName("Linha11")
  val Linha11: String,
  @SerializedName("Linha12")
  val Linha12: String,
  @SerializedName("Linha13")
  val Linha13: String,
  @SerializedName("Linha14")
  val Linha14: String,
  @SerializedName("Linha15")
  val Linha15: String,
  @SerializedName("Linha16")
  val Linha16: String,
  @SerializedName("Linha17")
  val Linha17: String,
  @SerializedName("Linha18")
  val Linha18: String,
  @SerializedName("Linha19")
  val Linha19: String,
  @SerializedName("Linha20")
  val Linha20: String,
)