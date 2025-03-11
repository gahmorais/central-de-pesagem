package br.com.backupautomacao.centraldepesagem

import android.app.Application
import br.com.backupautomacao.centraldepesagem.di.mainModule
import br.com.backupautomacao.centraldepesagem.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class AppApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidLogger(Level.DEBUG)
      androidContext(this@AppApplication)
      modules(mainModule, viewModelModule)
    }
    Timber.plant(Timber.DebugTree())
  }
}