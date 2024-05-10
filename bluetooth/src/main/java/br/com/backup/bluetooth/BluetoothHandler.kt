package br.com.backup.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import java.io.IOException
import kotlin.jvm.Throws

class BluetoothHandler {
  @Throws(SecurityException::class)
  fun getAvailableDevices(adapter: BluetoothAdapter): MutableSet<BluetoothDevice> =
    adapter.bondedDevices

  @Throws(SecurityException::class)
  suspend fun connectToDevice(device: BluetoothDevice): BluetoothConnected = try {
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