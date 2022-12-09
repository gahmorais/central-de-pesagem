package br.com.backupautomacao.centraldepesagem

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import java.io.*
import java.util.*

class FileController {

  @Throws(IOException::class)
  fun readFileFromDownloadFolder(filename: String): String {
    try {
      val downloadFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val file = File(downloadFolder, filename)
      val bufferedReader = BufferedReader(FileReader(file))
      val readingFile = Scanner(bufferedReader)
      val text = StringBuilder()
      while (readingFile.hasNext()) {
        text.append(readingFile.nextLine())
      }

      return text.toString()
    } catch (e: IOException) {
      throw e
    } catch (e: Exception) {
      throw e
    }
  }
}
