package br.com.backupautomacao.pesagem_modular

import android.app.Application
import androidx.work.WorkManager
import br.com.backupautomacao.pesagem_modular.di.mainModule
import br.com.backupautomacao.pesagem_modular.di.networkModule
import br.com.backupautomacao.pesagem_modular.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AppApplication : Application() {

  val workManager by lazy { WorkManager.getInstance(this) }
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidLogger(Level.DEBUG)
      androidContext(this@AppApplication)
      modules(listOf(mainModule, networkModule, viewModelModule))
    }
  }
}