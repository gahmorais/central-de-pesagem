package br.com.backupautomacao.bluetooth

import android.bluetooth.BluetoothSocket
import br.com.backupautomacao.bluetooth.exceptions.ClosedSocketException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.io.IOException

class BluetoothConnected(private val socket: BluetoothSocket) {
  fun write(data: ByteArray) {
    try {
      socket.outputStream.write(data)
    } catch (e: IOException) {
      e.printStackTrace()
      Timber.d("Não foi possível enviar os dados")
      try {
        Timber.d("Tentando encerrar o socket")
        socket.close()
      } catch (e: Exception) {
        e.printStackTrace()
        Timber.d("Não foi possível encerrar o socket")
        throw ClosedSocketException("Não foi possível encerrar o socket")
      }
    } catch (e: Exception) {
      throw e
    }
  }

  fun read(): Flow<ByteArray> {
    return flow {
      var numBytes: Int
      val buffer = ByteArray(1024)
      while (true) {
        numBytes = socket.inputStream.available()
        if (numBytes != 0) {
          delay(100)
          numBytes = socket.inputStream.read(buffer)
          val data = buffer.copyOf(numBytes)
          if (data.size > 1){
            emit(data)
          }
        }
      }
    }.catch { e ->
      e.printStackTrace()
      throw e
    }.flowOn(Dispatchers.IO)
  }
}