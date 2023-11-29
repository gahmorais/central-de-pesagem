package br.com.backupautomacao.pesagem_modular.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import br.com.backupautomacao.pesagem_modular.ui.UiHandler
import java.util.concurrent.Executors

class AndroidBluetoothController(context: Context) {
  private val bluetoothManager by lazy {
    context.getSystemService(BluetoothManager::class.java)
  }
  private val bluetoothAdapter by lazy {
    bluetoothManager?.adapter
  }

  @Throws(SecurityException::class)
  fun getPairedDevices(): List<BluetoothDevice> {
    return bluetoothAdapter
      ?.bondedDevices
      ?.filter { it.name.contains("BT_URA") || it.name.contains("BKP") }
      ?.sortedByDescending { it.address } ?: listOf()
  }

  fun connectToDevices(devices: List<BluetoothDevice>, bluetoothListener: BluetoothListener) {
    val uiHandler = UiHandler(bluetoothListener)
    devices.forEach { device ->
      bluetoothAdapter?.apply {
        val executor = Executors.newSingleThreadExecutor()
        val newThread = ConnectingThread(this, device.address, uiHandler)
        executor.execute(newThread)
        executor.shutdown()
      }
    }
  }
}