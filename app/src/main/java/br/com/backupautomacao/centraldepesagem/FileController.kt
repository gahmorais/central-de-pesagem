package br.com.backupautomacao.centraldepesagem

import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

class FileController {

  @Throws(IOException::class)
  fun readFileFromDownloadFolder(filename: String): String {
    try {
      val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val file = File(downloadFolder, filename)
      val bufferedReader = BufferedReader(FileReader(file))
      val readingFile = Scanner(bufferedReader)
      val text = StringBuilder()
      while (readingFile.hasNext()) {
        text.append(readingFile.nextLine())
      }

      return String(text)
    } catch (e: IOException) {
      throw e
    }
  }
}
