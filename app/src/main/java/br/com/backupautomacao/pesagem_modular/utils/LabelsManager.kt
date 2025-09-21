package br.com.backupautomacao.pesagem_modular.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object LabelManager {

    private const val LABELS_FOLDER = "labels"

    fun ensureLabelsDirectory(context: Context) {
        val externalDir = getExternalLabelsDir(context)
        Log.d("LabelManager", "Caminho da pasta: ${externalDir.absolutePath}")

        if (!externalDir.exists()) {
            Log.d("LabelManager", "Pasta labels não existe. Criando...")
            val created = externalDir.mkdirs()
            Log.d("LabelManager", "Pasta criada: $created")
        }
            copyAssetsToExternalStorage(context, LABELS_FOLDER, externalDir)
    }


    fun listExternalLabelFiles(context: Context): List<String> {
        val dir = getExternalLabelsDir(context)
        return dir.listFiles()
            ?.filter { it.isFile && it.extension == "zpl" }
            ?.map { it.name }
            ?: emptyList()
    }
    fun getExternalLabelsDir(context: Context): File {
        val mediaDir = File(
            Environment.getExternalStorageDirectory(),
            "Android/media/${context.packageName}/files/labels"
        )

        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }

        return mediaDir
    }



    private fun copyAssetsToExternalStorage(context: Context, assetPath: String, destDir: File) {
        val assetManager = context.assets
        val files = assetManager.list(assetPath) ?: return

        Log.d("LabelManager", "Copiando arquivos de assets/$assetPath: ${files.joinToString()}")

        for (filename in files) {
            val inputStream = assetManager.open("$assetPath/$filename")
            val outFile = File(destDir, filename)

            inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                    Log.d("LabelManager", "Arquivo copiado: $filename")
                }
            }
        }
    }

}