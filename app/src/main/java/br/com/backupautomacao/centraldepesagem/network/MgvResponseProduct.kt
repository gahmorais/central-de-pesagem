package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseProduct(
  @SerializedName("ConsultaItemResult")
  val mgvError: MgvError?,
  val item: MgvProduct?
)

data class MgvProduct(
  var nutrictionFacts: MgvInformacoesNutricionais? = null,
  var extraInfo: ExtraInfo? = null,
  @SerializedName("Codigo")
  val code: String = "",
  @SerializedName("Descritivo1aLinha")
  val description: String = "",
  @SerializedName("CodInfoExtra")
  val codExtraInfo: Int? = null,
  @SerializedName("CodInfoNutricional")
  val codNutrictionFacts: Int? = null,
  @SerializedName("CodTara")
  val codTara: Int? = null,
  @SerializedName("DiasValidade")
  val shelfLife: Int = 0,
  @SerializedName("Preco")
  val price: Double = 0.0,
  @SerializedName("Lote")
  val batch: String = "",
  @SerializedName("PrecoPromocional")
  val promotionalPrice: Double = 0.0,
  var weight: Double? = 0.0,
  var totalPrice: Double? = 0.0
)