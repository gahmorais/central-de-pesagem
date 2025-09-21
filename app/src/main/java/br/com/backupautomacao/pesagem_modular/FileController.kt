package br.com.backupautomacao.pesagem_modular

import android.content.Context
import java.io.*
import java.util.*

class FileController(private val context: Context) {

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
}
