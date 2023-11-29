package br.com.backupautomacao.pesagem_modular.di

import androidx.room.Room
import androidx.work.WorkManager
import br.com.backupautomacao.pesagem_modular.bluetooth.AndroidBluetoothController
import br.com.backupautomacao.pesagem_modular.db.AppDatabase
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.db.repository.OperationRepository
import br.com.backupautomacao.pesagem_modular.labels.LabelBuilder
import br.com.backupautomacao.pesagem_modular.network.MgvRepository
import br.com.backupautomacao.pesagem_modular.utils.StringResourceProvider
import org.koin.dsl.module

val mainModule = module {
  factory { WorkManager.getInstance(get()) }
  single { ConfigRepository(context = get()) }
  single { AndroidBluetoothController(get()) }
  single { LabelBuilder(get()) }
  single { StringResourceProvider(get()) }
  single { MgvRepository() }
  single {
    Room.databaseBuilder(
      get(),
      AppDatabase::class.java,
      "app_database"
    ).build()
  }
  single { OperationRepository(get<AppDatabase>().operationDao()) }
}