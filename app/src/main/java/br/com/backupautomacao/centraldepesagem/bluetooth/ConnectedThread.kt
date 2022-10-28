package br.com.backupautomacao.centraldepesagem.bluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_ERROR_MESSAGE
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CODE_MESSAGE_READ
import br.com.backupautomacao.centraldepesagem.Constants.Companion.COMMUNICATTION_ERROR
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import java.io.IOException
import java.nio.charset.StandardCharsets

class ConnectedThread(private val socket: BluetoothSocket, private val handler: Handler) :
  Thread() {
  private val mInputStream = socket.inputStream
  private val mOutputStream = socket.outputStream

  override fun run() {
    super.run()
    val buffer = ByteArray(1024)
    var bytes: Int
    while (true) {
      try {
        bytes = mInputStream.available()
        if (bytes != 0) {
          SystemClock.sleep(100)
          bytes = mInputStream.read(buffer)
          val data = buffer.copyOf(bytes)
          val hasPrintableChar = Formatter
            .removeNonAlphaCharacters(String(data, StandardCharsets.UTF_8))
            .isNotEmpty()
          Log.i("ConnectedThread", "run: ${String(data, StandardCharsets.UTF_8)}")
          if (hasPrintableChar) {
            handler.obtainMessage(CODE_MESSAGE_READ, -1, -1, data).sendToTarget()
          }
        }
      } catch (e: IOException) {
        e.printStackTrace()
        break
      }
    }
  }

  fun write(data: ByteArray) {
    try {
      mOutputStream.write(data)
    } catch (e: Exception) {
      handler.obtainMessage(CODE_ERROR_MESSAGE, -1, -1, COMMUNICATTION_ERROR)
      Log.e("ConnectedThread", "write: Erro", e)
    }
  }

  fun cancel() {
    try {
      socket.close()
    } catch (e: Exception) {
      Log.e("ConnectedThread", "cancel: Erro", e)
    }
  }
}