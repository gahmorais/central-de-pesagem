package br.com.backupautomacao.pesagem_modular.ui

import android.os.Handler
import android.os.Looper
import android.os.Message
import br.com.backupautomacao.pesagem_modular.Constants
import br.com.backupautomacao.pesagem_modular.bluetooth.BluetoothListener
import br.com.backupautomacao.pesagem_modular.bluetooth.ConnectedThread

class UiHandler(private val listener: BluetoothListener) : Handler(Looper.getMainLooper()) {
  override fun handleMessage(msg: Message) {
    super.handleMessage(msg)

    when (msg.what) {
      Constants.CODE_MESSAGE_READ -> listener.onBluetoothRead(msg.obj as ByteArray)
      Constants.CODE_CONNECTING_STATUS -> listener.onBluetoothConnecting()
      Constants.CODE_CONNECTED_STATUS -> listener.onBluetoothConnected(msg.obj as ConnectedThread)
      Constants.CODE_ERROR_MESSAGE -> listener.onBluetoothError(msg.obj as String)
    }
  }
}