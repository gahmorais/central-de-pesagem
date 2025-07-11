package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseNutritionalInfo(
    @SerializedName("informacoesNutricionais")
    val nutrictionFacts: List<MgvInformacoesNutricionais>?,
    @SerializedName("ExportaInformacaoNutricionalResult")
    val mgvError: MgvError?
)

//AÇUCARES ADICIONADOS DOUBLE
data class MgvInformacoesNutricionais(
    val Codigo: String?,
    val Carboidrato: Double?,
    val AcucaresTotais: Double?,
    val AcucaresAdicionado: Double?,
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