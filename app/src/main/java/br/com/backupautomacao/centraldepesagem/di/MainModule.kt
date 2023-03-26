package br.com.backupautomacao.centraldepesagem.di

import androidx.room.Room
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.db.AppDatabase
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import org.koin.dsl.module

val mainModule = module {
  factory { WorkManager.getInstance(get()) }
  single { ConfigRepository(context = get()) }
  single {
    Room.databaseBuilder(
      get(),
      AppDatabase::class.java,
      "app_database"
    ).build()
  }
  single { OperationRepository(get<AppDatabase>().operationDao()) }
}