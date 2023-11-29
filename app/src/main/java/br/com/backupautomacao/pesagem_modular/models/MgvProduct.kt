package br.com.backupautomacao.pesagem_modular.models

interface MgvProduct {
  var nutrictionFacts: MgvNutritionalInfo?
  var extraInfo: MgvExtraInfo?
  val code: String
  val description: String
  val codExtraInfo: Int?
  val codNutrictionFacts: Int?
  val codTara: Int?
  val shelfLife: Int
  val price: Double
  val batch: String
  val promotionalPrice: Double
  var totalPrice: Double?
}