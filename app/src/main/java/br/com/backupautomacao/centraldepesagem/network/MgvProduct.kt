package br.com.backupautomacao.centraldepesagem.network

data class MgvProduct(
  val Codigo: String = "",
  val Descritivo1aLinha: String = "",
  val CodInfoExtra: Int = 0,
  val DiasValidade: Int = 0,
  val Preco: Double = 0.0,
  val Lote: String = "",
  val PrecoPromocional: Double = 0.0,
  var weight: Double? = 0.0
)