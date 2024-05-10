package br.com.backupautomacao.pesagem_modular.utils

import br.com.backup.bluetooth.BluetoothConnected
import br.com.backup.bluetooth.exceptions.ClosedSocketException

class ScaleFunctions {
  private fun loadPrice(value: Double, device: BluetoothConnected) {
    val priceByteArray = Formatter.formatDataToLoadPriceToledo(value)
    device.write(priceByteArray)
  }

  fun weighing(btConnected: BluetoothConnected) {
    try {
      val command = byteArrayOf(0x05)
      btConnected.write(command)
//    updateWeightProduct("5000".toByteArray())
    } catch (e: ClosedSocketException) {
      throw e
    } catch (e: Exception) {
      throw e
    }
  }
}