package br.com.backupautomacao.centraldepesagem

import android.app.Application
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.di.mainModule
import br.com.backupautomacao.centraldepesagem.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AppApplication : Application() {

//  private val database by lazy { AppDatabase.getDatabase(this) }
//  val operationRepository by lazy { OperationRepository(database.operationDao()) }
//  val configRepository by lazy { ConfigRepository(this) }
  val workManager by lazy { WorkManager.getInstance(this) }
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidLogger(Level.DEBUG)
      androidContext(this@AppApplication)
      modules(listOf(mainModule, networkModule))
    }
  }
}