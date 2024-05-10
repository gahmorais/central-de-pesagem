package br.com.backupautomacao.pesagem_modular.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.backupautomacao.pesagem_modular.data.dao.ConfigDao
import br.com.backupautomacao.pesagem_modular.data.dao.LabelDao
import br.com.backupautomacao.pesagem_modular.data.dao.OperationDao
import br.com.backupautomacao.pesagem_modular.data.model.Config
import br.com.backupautomacao.pesagem_modular.data.model.Label
import br.com.backupautomacao.pesagem_modular.data.model.Operation

const val DATABASE_NAME = "app_database"

@Database(
  entities = [Operation::class, Label::class, Config::class],
  version = 3,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun operationDao(): OperationDao
  abstract fun labelDao(): LabelDao
  abstract fun configDao(): ConfigDao

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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `configuracoes` (`nome`TEXT NOT NULL, `valor` TEXT NOT NULL, PRIMARY KEY(`nome`))"
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
          .addMigrations(MIGRATION_2_3)
//          .createFromAsset("app_database.db")
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}