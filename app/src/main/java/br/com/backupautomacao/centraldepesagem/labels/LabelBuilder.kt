package br.com.backupautomacao.centraldepesagem.labels

import android.content.Context
import br.com.backupautomacao.centraldepesagem.FileController

class LabelBuilder {

  companion object {
    fun build(label: String, context: Context): Label {
      return when (label) {
        "ET119" -> ET119(context, label)
        "ET313" -> ET313(context, label)
        else -> throw Exception("Modelo de etiqueta não encontrado")
      }
    }
  }

  fun listLabels(context: Context): List<String> {
    val fileController = FileController(context)
    return fileController.readFilesFromAssets("labels")
  }

}