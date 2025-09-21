package br.com.backupautomacao.pesagem_modular.utils

object UnitOfMeasure {
    const val UNIDADE = 1
    const val PESO = 0

    fun fromCodigo(codigo: Int?): Int? {
        return when (codigo) {
            UNIDADE -> UNIDADE
            PESO -> PESO
            else -> null
        }
    }
}