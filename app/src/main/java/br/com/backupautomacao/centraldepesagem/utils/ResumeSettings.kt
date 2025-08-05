package br.com.backupautomacao.centraldepesagem.utils

data class ResumeSettings(
    val automaticPrint: Boolean,
    val serverAddress: String?,
    val printerAddress: String?,
    val printerPort: Int,
    val storeNumber: Int,
    val printerType: String
)