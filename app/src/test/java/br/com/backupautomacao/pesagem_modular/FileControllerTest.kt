package br.com.backupautomacao.pesagem_modular

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FileControllerTest {
  @Test
  fun `should be return list files in label directory`(){
    val context = InstrumentationRegistry.getInstrumentation().context
    val fileController = FileController(context)
    val files = fileController.readFilesFromAssets("labels")
    Assert.assertArrayEquals(files.toTypedArray(), arrayOf<String>())
  }

  @Test
  fun `should be return content from file`(){
    val context = InstrumentationRegistry.getInstrumentation().context
    val fileController = FileController(context)
    val contentFile = fileController.readFileFromAssets("labels/etiqueta_base.prn")
    Assert.assertEquals(String::class, contentFile)
  }
}