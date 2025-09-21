package br.com.backupautomacao.pesagem_modular.di

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.room.Room
import androidx.work.WorkManager
import br.com.backupautomacao.bluetooth.BluetoothHandler
import br.com.backupautomacao.pesagem_modular.db.AppDatabase
import br.com.backupautomacao.pesagem_modular.db.migrations.MIGRATION_1_2
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.db.repository.OperationRepository
import br.com.backupautomacao.pesagem_modular.network.ApiPesagem
import br.com.backupautomacao.pesagem_modular.network.MgvApi
import br.com.backupautomacao.pesagem_modular.printers.Epson
import br.com.backupautomacao.pesagem_modular.printers.HoneywellPrinter
import br.com.backupautomacao.pesagem_modular.utils.Label
import br.com.backupautomacao.pesagem_modular.utils.ResourceProvider
import br.com.backupautomacao.pesagem_modular.R
import org.koin.dsl.module

val mainModule = module {
  single { ConfigRepository(get()) }
  single { OperationRepository(get<AppDatabase>().operationDao()) }
  single { WorkManager.getInstance(get()) }
  single { ResourceProvider(get()) }
  single {
    val usbManager = get<Context>().getSystemService(Context.USB_SERVICE) as UsbManager
    Epson(usbManager, get())
  }

  single {
    get<Context>().getSystemService(Context.USB_SERVICE) as UsbManager
  }
  single { HoneywellPrinter(get(), get(), get()) }
//  single { HoneywellPrinter(get(), get()) }


  single { Label(get()) }
  single { BluetoothHandler() }
  single {
    val address = get<ConfigRepository>().getConfig(ConfigRepository.SERVER_ADDRESS)
    val serverAddress = if (address.isNullOrEmpty()) {
      "http://127.0.0.1:9999"
    } else {
      address
    }

    val apiService = MgvApi.retrofitService(serverAddress)
    val password = get<ConfigRepository>().getTextConfig(ConfigRepository.PASSWORD_API)
    val store = get<ConfigRepository>().getIntConfig(ConfigRepository.STORE_NUMBER)

    ApiPesagem(apiService, password, store.toString())

  }
  single {
    Room.databaseBuilder(
      get(),
      AppDatabase::class.java,
      get<Context>().getString(R.string.database_name) //DATABASE_NAME
    ).addMigrations(MIGRATION_1_2)
      .createFromAsset("app_database.db")
      .build()
  }
//  single {
//    val bluetoothManager =
//      get<Context>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//    bluetoothManager.adapter
//  }
}