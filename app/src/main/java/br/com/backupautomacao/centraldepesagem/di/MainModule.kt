package br.com.backupautomacao.centraldepesagem.di

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.room.Room
import androidx.work.WorkManager
import br.com.backupautomacao.bluetooth.BluetoothHandler
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.db.AppDatabase
import br.com.backupautomacao.centraldepesagem.db.migrations.MIGRATION_1_2
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.printers.Epson
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider
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

  single { Label(get()) }
  single { BluetoothHandler() }
  single {
    val address = get<ConfigRepository>()
      .getConfig(ConfigRepository.SERVER_ADDRESS)

    val serverAddress = if (address.isNullOrEmpty()) {
      "http://127.0.0.1:9300"
    } else {
      address
    }

    MgvApi.retrofitService(serverAddress)
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