package br.com.backupautomacao.pesagem_modular.ui.activities

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.databinding.ActivityMainBinding
import br.com.backupautomacao.pesagem_modular.ui.ScreenSlidePagerAdapter
import br.com.backupautomacao.pesagem_modular.utils.UiUtils


class MainActivity : AppCompatActivity() {

  private val binding: ActivityMainBinding by lazy {
    ActivityMainBinding.inflate(layoutInflater)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    UiUtils.hideSystemUi(window)

    val pagerAdapter = ScreenSlidePagerAdapter(this)
    binding.viewPager.adapter = pagerAdapter

    binding.toolbar.apply {
      title = getString(R.string.app_name)
      setTitleTextColor(Color.WHITE)
      setSupportActionBar(this)
    }

    binding.imgClose.setOnClickListener {
      closeApplication()
    }
  }

  private fun closeApplication() {
    finish()
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    val viewPager = binding.viewPager
    if (viewPager.currentItem == 0) {
      super.onBackPressed()
    } else {
      viewPager.currentItem--
    }
  }
}