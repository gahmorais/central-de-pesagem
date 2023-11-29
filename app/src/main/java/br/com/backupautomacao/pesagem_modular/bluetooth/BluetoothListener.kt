package br.com.backupautomacao.pesagem_modular.bluetooth

interface BluetoothListener {
  fun onBluetoothRead(data: ByteArray)
  fun onBluetoothConnecting()
  fun onBluetoothConnected(device: ConnectedThread)
  fun onBluetoothError(message: String)
}