package br.com.backupautomacao.centraldepesagem.network

data class MgvResponseExtraInfo(
  val informacoesExtras: List<ExtraInfo>?,
  val ExportaInformacaoExtraResult: MgvError?
)

data class ExtraInfo(
  val Codigo: String,
  val Linha1: String,
  val Linha2: String,
  val Linha3: String,
  val Linha4: String,
  val Linha5: String,
  val Linha6: String,
  val Linha7: String,
  val Linha8: String,
  val Linha9: String,
  val Linha10: String,
  val Linha11: String,
  val Linha12: String,
  val Linha13: String,
  val Linha14: String,
  val Linha15: String,
  val Linha16: String,
  val Linha17: String,
  val Linha18: String,
  val Linha19: String,
  val Linha20: String,
)