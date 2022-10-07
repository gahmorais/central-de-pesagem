package br.com.backupautomacao.centraldepesagem.ui.activities

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.centraldepesagem.bluetooth.MediaButtonReceiver
import br.com.backupautomacao.centraldepesagem.databinding.ActivityMainBinding
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.ScreenSlidePagerAdapter
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory.MainFragmentViewModelFactory
import net.gotev.speech.Speech


class MainActivity : AppCompatActivity() {
  private val bindingView: ActivityMainBinding by lazy {
    ActivityMainBinding.inflate(layoutInflater)
  }

  private val mediaButtonReceiver by lazy {
    MediaButtonReceiver()
  }

  private val mainFragmentViewModel: MainFragmentViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(bindingView.root)

    val pagerAdapter = ScreenSlidePagerAdapter(this)
    bindingView.viewPager.adapter = pagerAdapter

  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    event?.run {
      if ((keyCode == KeyEvent.KEYCODE_HEADSETHOOK) ||
        (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY )||
        (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)) {
        mainFragmentViewModel.listening()
      }
    }
    Log.i("MainActivity", "onKeyUp: $event")
    return super.onKeyUp(keyCode, event)
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(mediaButtonReceiver)
  }

  override fun onBackPressed() {
    val viewPager = bindingView.viewPager
    if (viewPager.currentItem == 0) {
      super.onBackPressed()
    } else {
      viewPager.currentItem--
    }
  }
}