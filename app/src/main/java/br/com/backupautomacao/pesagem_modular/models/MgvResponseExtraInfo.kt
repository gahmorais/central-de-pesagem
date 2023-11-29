package br.com.backupautomacao.pesagem_modular.models

import br.com.backupautomacao.pesagem_modular.errors.MgvError
import com.google.gson.annotations.SerializedName

data class MgvResponseExtraInfo(
  @SerializedName("informacoesExtras")
  val mgvExtraInfos: List<MgvExtraInfo>?,
  @SerializedName("ExportaInformacaoExtraResult")
  val mgvError: MgvError?
)
