package br.com.backupautomacao.pesagem_modular.labels

import android.content.Context
import br.com.backupautomacao.pesagem_modular.FileController

class LabelBuilder(private val context: Context) {

  fun build(label: String): Label {
    return when (label) {
      "ET119" -> ET119(context, label)
      "ET313" -> ET313(context, label)
      else -> throw Exception("Modelo de etiqueta não encontrado")
    }
  }

  fun listLabels(): List<String> {
    val fileController = FileController(context)
    return fileController.readFilesFromAssets("labels")
  }

}