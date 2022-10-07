package br.com.backupautomacao.centraldepesagem.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MediaButtonReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    intent?.run {
      Log.i("MediaButtonReceiver", "onReceive: $action")
    }
  }
}