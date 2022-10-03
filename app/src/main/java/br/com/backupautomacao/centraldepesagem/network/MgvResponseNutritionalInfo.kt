package br.com.backupautomacao.centraldepesagem.network

data class MgvResponseNutritionalInfo(val informacoesNutricionais: List<MgvInformacoesNutricionais>, val ExportaInformacaoNutricionalResult: MgvError)