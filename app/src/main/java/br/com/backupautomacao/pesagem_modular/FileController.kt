package br.com.backupautomacao.pesagem_modular

import android.content.Context
import android.util.Log
import br.com.backupautomacao.pesagem_modular.exceptions.EmptyDataException
import java.io.*
import java.util.*

class FileController(private val context: Context) {

  fun readFileFromFolder(folder: String): String = try {
    val rootDir = context.getExternalFilesDir(folder)
    val files = rootDir?.listFiles()
    val labelModel = files?.find { file ->
      file.name.contains("config")
    }
    val content = StringBuilder()
    labelModel?.bufferedReader()?.useLines { lines ->
      lines.forEach { line ->
        Log.i("FileController", "Conteúdo arquivo: $line")
        content.append(line)
      }
    }
    if (content.isEmpty()) {
      throw EmptyDataException("Não há dados a serem impressos")
    }
    content.toString()
  } catch (e: IOException) {
    throw e
  } catch (e: Exception) {
    throw e
  }


  @Throws(IOException::class)
  fun readFileFromAssets(filename: String): String {

    try {
      val file = context.assets.open(filename)
      val readingFile = Scanner(file)
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

  @Throws(IOException::class)
  fun readFilesFromAssets(path: String): List<String> {
    try {
      return context
        .assets
        .list(path)
        ?.map { label ->
          label.substringBefore(".")
        } ?: listOf()
    } catch (e: Exception) {
      throw e
    }
  }
}
