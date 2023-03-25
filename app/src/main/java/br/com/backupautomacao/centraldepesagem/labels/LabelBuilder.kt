package br.com.backupautomacao.centraldepesagem.labels

import android.content.Context
import br.com.backupautomacao.centraldepesagem.FileController

class LabelBuilder() {
  fun listLabels(context: Context) : List<String>{
    val fileController = FileController(context)
    return fileController.readFilesFromAssets("labels")
  }
}