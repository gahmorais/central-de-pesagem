package br.com.backupautomacao.centraldepesagem.models

import br.com.backupautomacao.centraldepesagem.errors.MgvError
import com.google.gson.annotations.SerializedName

data class MgvResponseExtraInfo(
  @SerializedName("informacoesExtras")
  val mgvExtraInfos: List<MgvExtraInfo>?,
  @SerializedName("ExportaInformacaoExtraResult")
  val mgvError: MgvError?
)
