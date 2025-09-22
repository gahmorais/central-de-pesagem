package br.com.backupautomacao.pesagem_modular.ui.activities

import android.graphics.Color
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.databinding.ActivityMainBinding
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.printers.Epson
import br.com.backupautomacao.pesagem_modular.printers.HoneywellPrinter
import br.com.backupautomacao.pesagem_modular.ui.ScreenSlidePagerAdapter
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {
  private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

  private val ACTION_USB_PERMISSION = "br.com.backupautomacao.USB_PERMISSION"

  private val configRepository by inject<ConfigRepository>()
  private val viewModel by viewModel<MainViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    configRepository.initializeDefaults()
    setContentView(binding.root)

    lifecycleScope.launch {
      delay(3000)
      try {
        connectAndPrintUsb()
      } catch (e: Exception) {
        showMessage("Erro ao iniciar impressão: ${e.message}")
      }
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

  private fun connectAndPrintUsb() {
    val usbManager = getSystemService(USB_SERVICE) as UsbManager
    val config = viewModel.getUsbConfig()

    if (config == null) {
      showMessage("Nenhuma configuração USB definida")
      return
    }

    val parts = config.split(";")
    if (parts.size < 2) {
      showMessage("Configuração USB inválida")
      return
    }

    val model = parts[0]
    val deviceName = parts[1]

    val usbDevice = usbManager.deviceList.values.firstOrNull { it.deviceName == deviceName }
    if (usbDevice == null) {
      showMessage("Dispositivo USB '$deviceName' não encontrado")
      return
    }

    if (model.contains("TM-L100", ignoreCase = true)) {
      val epson = Epson(usbManager, this)
      epson.initializePrinter(config)

      val product = viewModel.getCurrentProduct()
      if (product == null) {
        showMessage("Produto inválido ou nulo.")
        return
      }

      val created = epson.createReceiptData(product)
      if (created) {
        val success = epson.printData()
        showMessage(if (success) "Impressão enviada com sucesso (Epson)!" else "Erro ao imprimir na Epson")
      } else {
        showMessage("Erro ao criar dados de impressão para Epson")
      }

    } else {

      val printer = HoneywellPrinter(this, usbManager, get<ConfigRepository>())
//      val printer = HoneywellPrinter(usbManager ,this)

      printer.initializePrinter(usbDevice)
      if (!printer.hasPermission()) {
        printer.requestPermissionAndConnect(usbDevice) { success, message ->
          showMessage(message)
          if (success) {
            printLabel(printer)
          }
        }
        return
      }

      val connected = printer.connect()
      if (!connected) {
        showMessage("Falha ao conectar com a impressora Honeywell.")
        return
      }

      printLabel(printer)

    }
  }

  private fun printLabel(printer: HoneywellPrinter) {
    val product = viewModel.getCurrentProduct()
    if (product == null) {
      showMessage("Produto inválido ou nulo.")
      return
    }

    lifecycleScope.launch {
      try {
        printer.print(product)
        showMessage("Impressão enviada com sucesso!")
      } catch (e: Exception) {
        Timber.e(e, "Erro ao imprimir")
        showMessage("Erro ao imprimir: ${e.message}")
      } finally {
        printer.disconnect()
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

//HONEYWELL
/*
private fun connectAndPrintUsb() {
  val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
  val devices = usbManager.deviceList.values

  if (devices.isEmpty()) {
    showMessage("Nenhum dispositivo USB encontrado")
    return
  }

  val device = devices.first()
  val printer = HoneywellPrinter(usbManager, this)

  printer.initializePrinter(device)

  if (printer.hasPermission()) {
    val connected = printer.connect()
    if (connected) {
      printLabel(printer)
    } else {
      showMessage("Falha ao conectar com a impressora.")
    }
  } else {
    printer.requestPermissionAndConnect(device) { success, message ->
      showMessage(message)
      if (success) {
        printLabel(printer)
      }
    }
  }
}
*/

//PRIMEIRO MODELO
/*
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.ActivityMainBinding
import br.com.backupautomacao.centraldepesagem.ui.ScreenSlidePagerAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {
  private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

  private val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"

  private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ACTION_USB_PERMISSION -> {
          val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            Timber.d("Permissão concedida para o dispositivo: ${device?.deviceName}")
            showMessage("Permissão concedida: ${device?.deviceName}")
          } else {
            Timber.d("Permissão negada para o dispositivo USB")
            showMessage("Permissão USB negada.")
          }
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)

    val filter = IntentFilter(ACTION_USB_PERMISSION)
    registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

    lifecycleScope.launch {
      delay(3000)
      try {
        requestPermissionUSB(this@MainActivity)
      } catch (e: Exception) {
        e.message?.let { showMessage(it) }
      }
    }

    /*
    try {
      requestPermissionUSB(this)
    } catch (e: Exception) {
      e.message?.let { showMessage(it) }
    }

     */

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

  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(usbReceiver)
  }

  private fun requestPermissionUSB(context: Context) {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val devices = usbManager.deviceList.values

    if (devices.isEmpty()) {
      throw Exception("Nenhum dispositivo USB encontrado")
    }

    val device = devices.first()
    val permissionIntent = PendingIntent.getBroadcast(
      context,
      0,
      Intent(ACTION_USB_PERMISSION),
      PendingIntent.FLAG_IMMUTABLE
    )

    if (!usbManager.hasPermission(device)) {
      usbManager.requestPermission(device, permissionIntent)
    } else {
      Timber.d("Permissão já concedida para o dispositivo: ${device.deviceName}")
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
*/