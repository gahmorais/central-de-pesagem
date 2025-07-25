package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class MgvResponseNutritionalInfo(
    @SerializedName("informacao_nutricional")
    val nutrictionFacts: List<MgvInformacoesNutricionais>?,
    @SerializedName("ExportaInformacaoNutricionalResult")
    val mgvError: MgvError?
)
data class MgvInformacoesNutricionais(
    val Codigo: String?,
    @SerializedName("codigo")
    val code: String,
    @SerializedName("observacao")
    val observacao: String,
    //100G
    @SerializedName("100g_valor_energetico")
    val ValorEnergetico_100g: String?,
    @SerializedName("100g_carboidratos")
    val Carboidrato_100g: String?,
    @SerializedName("100g_acucares_totais")
    val Acucares_Totais_100g: String?,
    @SerializedName("100g_acucares_adicionados")
    val Acucares_Adicionado_100g: String?,
    @SerializedName("100g_proteinas")
    val Proteina_100g: String?,
    @SerializedName("100g_gorduras_totais")
    val Gordura_Totais_100g: String?,
    @SerializedName("100g_gorduras_saturadas")
    val Gordura_Saturada_100g: String?,
    @SerializedName("100g_gorduras_trans")
    val GorduraTrans_100g: String?,
    @SerializedName("100g_fibra_alimentar")
    val FibraAlimentar_100g: String?,
    @SerializedName("100g_sodio")
    val Sodio_100g: String?,

    //PORÇÃO
    @SerializedName("porcao_valor_energetico")
    val Porcao_ValorEnergetico: String?,
    @SerializedName("porcao_carboidratos")
    val Porcao_Carboidrato: String?,
    @SerializedName("porcao_acucares_totais")
    val Porcao_AcucaresTotais: String?,
    @SerializedName("porcao_acucares_adicionados")
    val Porcao_AcucaresAdicionado: String?,
    @SerializedName("porcao_proteinas")
    val Porcao_Proteina:  String?,
    @SerializedName("porcao_gorduras_totais")
    val Porcao_GorduraTotais: String?,
    @SerializedName("porcao_gorduras_saturadas")
    val Porcao_GorduraSaturada: String?,
    @SerializedName("porcao_gorduras_trans")
    val Porcao_GorduraTrans: String?,
    @SerializedName("porcao_fibra_alimentar")
    val Porcao_FibraAlimentar: String?,
    @SerializedName("porcao_sodio")
    val Porcao_Sodio: String?,

    //VD
    @SerializedName("porcentagem_vd_valor_energetico")
    val Porcentagem_vd_ValorEnergetico: String?,
    @SerializedName("porcentagem_vd_carboidratos")
    val Porcentagem_vd_Carboidrato: String?,
    @SerializedName("porcentagem_vd_acucares_totais")
    val Porcentagem_vd_AcucaresTotais: String?,
    @SerializedName("porcentagem_vd_acucares_adicionados")
    val Porcentagem_vd_AcucaresAdicionado: String?,
    @SerializedName("porcentagem_vd_proteinas")
    val Porcentagem_vd_Proteina: String?,
    @SerializedName("porcentagem_vd_gorduras_totais")
    val Porcentagem_vd_GorduraTotais: String?,
    @SerializedName("porcentagem_vd_gorduras_saturadas")
    val Porcentagem_vd_GorduraSaturada: String?,
    @SerializedName("porcentagem_vd_gorduras_trans")
    val Porcentagem_vd_GorduraTrans: String?,
    @SerializedName("porcentagem_vd_fibra_alimentar")
    val Porcentagem_vd_FibraAlimentar: String?,
    @SerializedName("porcentagem_vd_sodio")
    val Porcentagem_vd_Sodio: String?,

    //QUANTIDADE E UNIDADE
    @SerializedName("quantidade_porcoes_por_embalagem")
    val Quantidade_Porcoes_Por_Embalagem : String?,
    @SerializedName("quantidade_porcao")
    val Quantidade_porcao : String?,
    @SerializedName("unidade_porcao")
    val Unidade_porcao : String?,
    @SerializedName("quantidade")
    val Quantidade : String?,
    @SerializedName("reservado")
    val Reservado : String?,
    @SerializedName("ehRDC429")
    val EhRDC429  : Boolean?,

    //MEDIDAS
    @SerializedName("parte_inteira_medida_caseira")
    val Parte_Inteira_Medida_Caseira : String?,
    @SerializedName("parte_decimal_medida_caseira")
    val Parte_Decimal_Medida_Caseira : String?,
    @SerializedName("medida_caseira_utilizada")
    val Medida_Caseira_Utilizada: String?,

    //IMAGENS DE ALTO EM ...
    @SerializedName("alto_em_acucar_adicionado")
    val Alto_Acucar_Add : Boolean?,
    @SerializedName("alto_em_gordura_saturada")
    val Alto_Gordura_Saturada : Boolean?,
    @SerializedName("alto_em_sodio")
    val Alto_Sodio : Boolean?,

    //LACTOSE
    @SerializedName("lactose")
    val Lactose : String?,
    @SerializedName("galactose")
    val Galactose : String?,
    @SerializedName("imprime_lactose_galactose")
    val Imprime_Lactose_Galactose : String?,

    //ESTENDIDOS
    @SerializedName("acucares_adicionados_estendido")
    val Acucar_Add_Estendido : String?,
    @SerializedName("acucares_totais_estendido")
    val Acucar_Totais_Estendido : String?,
    @SerializedName("gorduras_totais_estendido")
    val Gordura_Totais_Estendido : String?,
    @SerializedName("proteinas_estendido")
    val Proteinas_Estendido : String?,

    //FRAÇÃO
    @SerializedName("utiliza_fracao_personalizada")
    val Utiliza_Fracao_Personalizada : String?,
    @SerializedName("numerador_fracao")
    val Numerador_Fracao : String?,
    @SerializedName("denominador_fracao")
    val Denominador_Fracao : String?,

)