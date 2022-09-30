package br.com.backupautomacao.centraldepesagem.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CONNECTED_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CONNECTING_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_MESSAGE

class ConnectingThread(
  private val bluetoothAdapter: BluetoothAdapter,
  private val deviceAddress: String,
  private val handler: Handler
) : Thread() {
  private val TAG = "ConnectingThread"
  private var mBTSocket: BluetoothSocket? = null
  @Throws(SecurityException::class)
  override fun run() {
    var fail = false
    val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
    try {
      mBTSocket = createBluetoothSocket(device)
    } catch (e: Exception) {
      fail = true
      handler.obtainMessage(ERROR_MESSAGE,-1,-1, ).sendToTarget()
      Log.e(TAG, "run: Falha ao criar socket", e)
    }

    try{
      mBTSocket?.connect()
      handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget()
    }catch (e: Exception){
      fail = true
      handler.obtainMessage(ERROR_MESSAGE,-1,-1,"Falha ao realizar conexão").sendToTarget()
      try{
        mBTSocket?.close()
        Log.e(TAG, "run: Falha ao realizar conexão", e)
      }catch (e: Exception){
        handler.obtainMessage(ERROR_MESSAGE,-1,-1,"Falha ao fechar o socket").sendToTarget()
      }
    }

    if(!fail && mBTSocket != null){
      val connectedThread = ConnectedThread(mBTSocket!!, handler)
      connectedThread.start()
      handler.obtainMessage(CONNECTED_STATUS, 1,-1,connectedThread).sendToTarget()
      mBTSocket = null
      interrupt()
    }
  }

  @Throws(SecurityException::class)
  private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
    return device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
  }
}