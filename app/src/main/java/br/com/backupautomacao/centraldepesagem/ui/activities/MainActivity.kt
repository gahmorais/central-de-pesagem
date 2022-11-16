package br.com.backupautomacao.centraldepesagem.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.backupautomacao.centraldepesagem.databinding.ActivityMainBinding
import br.com.backupautomacao.centraldepesagem.ui.ScreenSlidePagerAdapter


class MainActivity : AppCompatActivity() {
  private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    val pagerAdapter = ScreenSlidePagerAdapter(this)
    binding.viewPager.adapter = pagerAdapter
  }

  override fun onBackPressed() {
    val viewPager = binding.viewPager
    if (viewPager.currentItem == 0) {
      super.onBackPressed()
    } else {
      viewPager.currentItem--
    }
  }
}