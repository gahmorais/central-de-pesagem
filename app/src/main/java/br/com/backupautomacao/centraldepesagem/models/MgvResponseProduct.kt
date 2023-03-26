package br.com.backupautomacao.centraldepesagem.models

import br.com.backupautomacao.centraldepesagem.errors.MgvError
import com.google.gson.annotations.SerializedName

data class MgvResponseProduct(
  @SerializedName("ConsultaItemResult")
  val mgvError: MgvError?,
  val item: MgvProductImpl?
)