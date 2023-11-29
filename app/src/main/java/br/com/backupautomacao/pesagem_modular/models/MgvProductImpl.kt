package br.com.backupautomacao.pesagem_modular.models

import com.google.gson.annotations.SerializedName

data class MgvProductImpl(
  override var nutrictionFacts: MgvNutritionalInfo?,
  override var extraInfo: MgvExtraInfo?,
  @SerializedName("Codigo")
  override val code: String,
  @SerializedName("Descritivo1aLinha")
  override val description: String,
  @SerializedName("CodInfoExtra")
  override val codExtraInfo: Int?,
  @SerializedName("CodInfoNutricional")
  override val codNutrictionFacts: Int?,
  @SerializedName("CodTara")
  override val codTara: Int?,
  @SerializedName("DiasValidade")
  override val shelfLife: Int,
  @SerializedName("Preco")
  override val price: Double,
  @SerializedName("Lote")
  override val batch: String,
  @SerializedName("PrecoPromocional")
  override val promotionalPrice: Double,
  override var totalPrice: Double?,
  var unit: Int?,
  var weight: Double?
) : MgvProduct