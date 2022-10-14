package br.com.backupautomacao.centraldepesagem.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.viewModels
import br.com.backupautomacao.centraldepesagem.databinding.ActivityMainBinding
import br.com.backupautomacao.centraldepesagem.ui.ScreenSlidePagerAdapter
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel


class MainActivity : AppCompatActivity() {
  private val bindingView: ActivityMainBinding by lazy {
    ActivityMainBinding.inflate(layoutInflater)
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

  override fun onBackPressed() {
    val viewPager = bindingView.viewPager
    if (viewPager.currentItem == 0) {
      super.onBackPressed()
    } else {
      viewPager.currentItem--
    }
  }
}