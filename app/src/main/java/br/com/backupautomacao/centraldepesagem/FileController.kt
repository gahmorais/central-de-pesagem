package br.com.backupautomacao.centraldepesagem

import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*

class FileController {
  private val downloadFolder: File by lazy {
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
  }

  fun readFile(filename: String): String {
    val file = File(downloadFolder, filename)
    val bufferedReader = BufferedReader(FileReader(file))
    val readingFile = Scanner(bufferedReader)
    val text = StringBuilder()
    while (readingFile.hasNext()) {
      text.append(readingFile.nextLine())
    }

    return String(text)
  }
}