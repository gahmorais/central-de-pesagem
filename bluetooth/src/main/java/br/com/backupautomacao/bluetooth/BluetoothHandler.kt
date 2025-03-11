package br.com.backupautomacao.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import timber.log.Timber
import java.io.IOException

class BluetoothHandler {

  @Throws(SecurityException::class)
  fun getAvailableDevices(adapter: BluetoothAdapter): MutableSet<BluetoothDevice> =
    adapter.bondedDevices

  @Throws(SecurityException::class)
  suspend fun connectToDevice(device: BluetoothDevice): BluetoothConnected = try {
    Timber.d("Conectando ao dispositivo ${device.uuids[0].uuid}")
    val socket = device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
    socket.connect()
    BluetoothConnected(socket)
  } catch (e: IOException) {
    e.printStackTrace()
    throw e
  } catch (e: Exception) {
    e.printStackTrace()
    throw e
  }

}