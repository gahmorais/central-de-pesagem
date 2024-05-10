package br.com.backupautomacao.pesagem_modular.models

import br.com.backupautomacao.pesagem_modular.exceptions.MgvError
import com.google.gson.annotations.SerializedName

data class MgvResponseNutritionalInfo(
  @SerializedName("informacoesNutricionais")
  val nutrictionFacts: List<MgvNutritionalInfo>?,
  @SerializedName("ExportaInformacaoNutricionalResult")
  val mgvError: MgvError?
)

