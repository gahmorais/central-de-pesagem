package br.com.backupautomacao.centraldepesagem.permission

import android.Manifest
import android.Manifest.permission.*
import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.IllegalArgumentException

sealed class Permission(vararg val permissions: String) {

  @RequiresApi(Build.VERSION_CODES.S)
  object BluetoothAndroidS : Permission(BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT)
  object Bluetooth : Permission(BLUETOOTH, BLUETOOTH_ADMIN)
  object Record : Permission(RECORD_AUDIO)
  @RequiresApi(Build.VERSION_CODES.R)
  object Storage : Permission(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE)

  companion object {
    fun from(permission: String) = when (permission) {
      WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE -> Storage
      RECORD_AUDIO -> Record
      BLUETOOTH, BLUETOOTH_CONNECT, BLUETOOTH_ADMIN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BluetoothAndroidS
      } else {
        Bluetooth
      }
      else -> throw IllegalArgumentException("Unknow permission: $permission")
    }
  }
}