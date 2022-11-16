package br.com.backupautomacao.centraldepesagem

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.db.AppDatabase
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository

class AppApplication : Application() {
  private val database by lazy { AppDatabase.getDatabase(this) }
  val operationRepository by lazy { OperationRepository(database.operationDao()) }
  val workManager by lazy { WorkManager.getInstance(this) }
  val configRepository by lazy { ConfigRepository(this) }
}