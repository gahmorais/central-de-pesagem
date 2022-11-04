package br.com.backupautomacao.centraldepesagem.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.backupautomacao.centraldepesagem.db.dao.OperationDao
import br.com.backupautomacao.centraldepesagem.db.model.Operation

const val DATABASE_NAME = "app_database"

@Database(entities = [Operation::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
  abstract fun operationDao(): OperationDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          DATABASE_NAME
        ).build()
        INSTANCE = instance
        instance
      }
    }
  }
}