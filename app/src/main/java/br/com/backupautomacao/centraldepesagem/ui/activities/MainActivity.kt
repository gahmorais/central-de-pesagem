package br.com.backupautomacao.centraldepesagem.ui.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.ActivityMainBinding
import br.com.backupautomacao.centraldepesagem.db.AppDatabase
import br.com.backupautomacao.centraldepesagem.db.dao.OperationDao
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import br.com.backupautomacao.centraldepesagem.services.PrinterUsb
import br.com.backupautomacao.centraldepesagem.ui.ScreenSlidePagerAdapter
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory.MainViewModelFactory
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: MainFragmentViewModel by viewModels {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val printerUsb = PrinterUsb(usbManager)
        val configRepository = ConfigRepository(this)
        val appDataBase = AppDatabase.getDatabase(this)
        val operationRepository = OperationRepository(appDataBase.operationDao())
        val workManager = WorkManager.getInstance(this)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val resourceProvider = ResourceProvider(this)
        val label = Label(this)
        MainViewModelFactory(
            printerUsb, configRepository, operationRepository, workManager,
            bluetoothAdapter, resourceProvider, label
        )
    }
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
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(
                            "USB_PRINTER",
                            "Permissão concedida para o dispositivo: ${device?.deviceName}"
                        )
                    } else {
                        Log.d("USB_PRINTER", "Permissão negada para o dispositivo USB")
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

    override fun onBackPressed() {
        val viewPager = binding.viewPager
        if (viewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            viewPager.currentItem--
        }
    }
}