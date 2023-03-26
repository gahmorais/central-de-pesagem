package br.com.backupautomacao.centraldepesagem.models

import br.com.backupautomacao.centraldepesagem.errors.MgvError
import com.google.gson.annotations.SerializedName

data class MgvResponseNutritionalInfo(
  @SerializedName("informacoesNutricionais")
  val nutrictionFacts: List<MgvNutritionalInfo>?,
  @SerializedName("ExportaInformacaoNutricionalResult")
  val mgvError: MgvError?
)

