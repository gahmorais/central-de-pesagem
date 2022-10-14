package br.com.backupautomacao.centraldepesagem.network

data class MgvResponseNutritionalInfo(
  val informacoesNutricionais: List<MgvInformacoesNutricionais>?,
  val ExportaInformacaoNutricionalResult: MgvError?
)

data class MgvInformacoesNutricionais(
  val Codigo: String?,
  val Carboidrato: Double?,
  val DescricaoMedidaCaseira: Int?,
  val FibraAlimentar: Double?,
  val FracaoMedidaCaseira: Double?,
  val GorduraSaturada: Double?,
  val GorduraTotal: Double?,
  val GorduraTrans: Double?,
  val MedidaCaseira: Double?,
  val Observacao: String?,
  val Proteina: Double?,
  val QtdPorcao: Double?,
  val Sodio: Double?,
  val UnidadePorcao: Double?,
  val ValorEnergetico: Int?
)