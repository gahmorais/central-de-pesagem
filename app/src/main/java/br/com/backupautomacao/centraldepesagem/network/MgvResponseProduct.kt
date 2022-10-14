package br.com.backupautomacao.centraldepesagem.network

data class MgvResponseProduct(
  val ConsultaItemResult: MgvError?,
  val item: MgvProduct?
)

data class MgvProduct(
  var nutrictionFacts: MgvInformacoesNutricionais? = null,
  var extraInfo: ExtraInfo? = null,
  val Codigo: String = "",
  val Descritivo1aLinha: String = "",
  val CodInfoExtra: Int? = null,
  val CodInfoNutricional: Int? = null,
  val CodTara: Int? = null,
  val DiasValidade: Int = 0,
  val Preco: Double = 0.0,
  val Lote: String = "",
  val PrecoPromocional: Double = 0.0,
  var weight: Double? = 0.0,
  var totalPrice: Double? = 0.0
)