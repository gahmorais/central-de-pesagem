package br.com.backupautomacao.centraldepesagem.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.backupautomacao.centraldepesagem.db.dao.LabelDao
import br.com.backupautomacao.centraldepesagem.db.dao.OperationDao
import br.com.backupautomacao.centraldepesagem.db.model.Label
import br.com.backupautomacao.centraldepesagem.db.model.Operation

const val DATABASE_NAME = "app_database"

@Database(
  entities = [Operation::class, Label::class],
  version = 2,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun operationDao(): OperationDao
  abstract fun labelDao(): LabelDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null
    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `labels` (`id`INTEGER NOT NULL, `name` TEXT NOT NULL, `content` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          DATABASE_NAME
        )
          .addMigrations(MIGRATION_1_2)
          .createFromAsset("app_database.db")
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}