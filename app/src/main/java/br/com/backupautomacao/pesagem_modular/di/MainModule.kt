package br.com.backupautomacao.pesagem_modular.di

import androidx.room.Room
import androidx.work.WorkManager
import br.com.backup.bluetooth.BluetoothHandler
import br.com.backupautomacao.pesagem_modular.data.AppDatabase
import br.com.backupautomacao.pesagem_modular.data.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.data.repository.OperationRepository
import br.com.backupautomacao.pesagem_modular.labels.LabelBuilder
import br.com.backupautomacao.pesagem_modular.network.ApiMgv
import br.com.backupautomacao.pesagem_modular.utils.StringResourceProvider
import org.koin.dsl.module

val mainModule = module {
  factory { WorkManager.getInstance(get()) }
  single { ConfigRepository(get()) }
  single { BluetoothHandler() }
  single { LabelBuilder(get()) }
  single { StringResourceProvider(get()) }
  single { ApiMgv(get()) }
  single {
    Room.databaseBuilder(
      get(),
      AppDatabase::class.java,
      "app_database"
    ).build()
  }
  single { OperationRepository(get<AppDatabase>().operationDao()) }
}