package br.com.backupautomacao.pesagem_modular.models

import br.com.backupautomacao.pesagem_modular.errors.MgvError
import com.google.gson.annotations.SerializedName

data class MgvResponseProduct(
  @SerializedName("ConsultaItemResult")
  val mgvError: MgvError?,
  val item: MgvProductImpl?
)