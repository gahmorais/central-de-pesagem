package br.com.backupautomacao.tabletinteligente

import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.Executors

open class Printer(private val ipImpressora: String) : Runnable {
  private lateinit var dataToPrint: String

  override fun run() {
    val client = Socket(ipImpressora, 9100)
    println("Cliente conectado do IP: ${client.inetAddress.hostAddress}")
    val outputStream: OutputStream = client.getOutputStream()
    val pw = PrintWriter(outputStream)
    pw.write(dataToPrint)
    pw.close()
    client.close()
  }

  fun print(etiqueta: String) {
    dataToPrint = etiqueta
    val executor = Executors.newSingleThreadExecutor()
    executor.execute(this)
    executor.shutdown()
  }

}