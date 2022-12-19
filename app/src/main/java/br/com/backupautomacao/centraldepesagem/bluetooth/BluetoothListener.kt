package br.com.backupautomacao.centraldepesagem.bluetooth

interface BluetoothListener {
  fun onBluetoothRead(data: ByteArray)
  fun onBluetoothConnecting()
  fun onBluetoothConnected(device: ConnectedThread)
  fun onBluetoothError(message: String)
}