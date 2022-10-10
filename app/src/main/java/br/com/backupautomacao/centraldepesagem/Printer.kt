package br.com.backupautomacao.centraldepesagem

import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

open class Printer(private val printerAddress: String) : Runnable {
  private lateinit var dataToPrint: String

  override fun run() {
    val client = try {
      Socket(printerAddress, 9100)
    } catch (e: Exception) {
      throw e
    }
    println("Cliente conectado do IP: ${client.inetAddress.hostAddress}")
    val outputStream: OutputStream = client.getOutputStream()
    val pw = PrintWriter(outputStream)
    pw.write(dataToPrint)
    pw.close()
    client.close()
  }

  fun print(label: String) {
    dataToPrint = label
    val executor = Executors.newSingleThreadExecutor()
    try {
      val futureResult = executor.submit(this)
      futureResult.get()  //Pega o resultado da thread e
      //se ocorreu erro, lança a exceção ExecutionException
    } catch (e: ExecutionException) {
      throw e
    }finally {
      executor.shutdown()
    }
  }
}