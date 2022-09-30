package br.com.backupautomacao.centraldepesagem.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import br.com.backupautomacao.centraldepesagem.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
  private val bindingView: ActivityMainBinding by lazy {
    ActivityMainBinding.inflate(layoutInflater)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(bindingView.root)

    val pagerAdapter = ScreenSlidePagerAdapter(this)
    bindingView.viewPager.adapter = pagerAdapter
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