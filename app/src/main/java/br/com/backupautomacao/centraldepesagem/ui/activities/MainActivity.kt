package br.com.backupautomacao.centraldepesagem.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.ActivityMainBinding
import br.com.backupautomacao.centraldepesagem.ui.ScreenSlidePagerAdapter
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainViewModel
import br.com.backupautomacao.centraldepesagem.utils.parcelable
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {
  private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
  private val viewModel: MainViewModel by viewModel()

  private val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)

    try {
      viewModel.requestPermissionUSB(this)
    } catch (e: Exception) {
      e.message?.let { showMessage(it) }
    }

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

  private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ACTION_USB_PERMISSION -> {
          val device = intent.parcelable(UsbManager.EXTRA_DEVICE) as UsbDevice?
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            Timber.d("Permissão concedida para o dispositivo: ${device?.deviceName}")
          } else {
            Timber.d("Permissão negada para o dispositivo USB")
          }
        }
      }
    }
  }

  private fun showMessage(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
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