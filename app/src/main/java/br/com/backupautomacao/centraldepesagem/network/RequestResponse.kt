package br.com.backupautomacao.centraldepesagem.network

import com.google.gson.annotations.SerializedName

data class RequestResponse(
    @SerializedName("ConsultaItemResult")
    val mgvError: MgvError?,
    val item: MgvProduct?
)

data class MgvProduct(
    @SerializedName("informacao_nutricional")
    var nutrictionFacts: MgvInformacoesNutricionais? = null,
    @SerializedName("informacao_extra")
    var extraInfo: ExtraInfo? = null,
    @SerializedName("informacao_fracionador")
    var fracionadorInfo: FracionadorInfo? = null,
    @SerializedName("informacao_fornecedor")
    var fornecedorInfo: FornecedorInfo? = null,
    @SerializedName("informacao_conservacao")
    var conservacaoInfo: ConservadorInfo? = null,
    var quantity: Int? = null,
    @SerializedName("codigo")
    val code: String = "",
    @SerializedName("descricao_primeira_linha")
    val description: String = "",
    @SerializedName("descricao_segunda_linha")
    val description2: String = "",
    @SerializedName("descricao_terceira_linha")
    val description3: String = "",
    @SerializedName("descricao_quarta_linha")
    val description4: String = "",
    @SerializedName("departamento")
    val department : String = "",
    @SerializedName("validade_dias")
    val shelfLife: Int = 0,
    @SerializedName("preco")
    val price: Double = 0.00,
    @SerializedName("lote")
    val batch: String = "",
    @SerializedName("preco_promocional")
    val promotionalPrice: Double = 0.00,
    var weight: Double? = 0.0,
    var totalPrice: Double? = 0.0,
    @SerializedName("tipo_produto")
    val typeProductRaw: String? = null
) {
    val unitOfMeasure: Int?
        get() = typeProductRaw?.toIntOrNull()
}

/*
//    @SerializedName("informacao_extra")
//    val codExtraInfo: Int? = null,
//    @SerializedName("informacao_conservacao")
//    val codConservacaoInfo: Int? = null,
//    @SerializedName("informacao_fornecedor")
//    val codFornecedorInfo: Int? = null,
//    @SerializedName("informacao_fracionador")
//    val codFracionadorInfo: Int? = null,
//    @SerializedName("informacao_nutricional")
//    val codNutrictionFacts: Int? = null,
//    @SerializedName("validade_dias")
//    @SerializedName("TipoVendaInt")
//    val unitOfMeasure: Int? = null
//)

 */