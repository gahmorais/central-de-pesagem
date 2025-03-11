package br.com.backupautomacao.centraldepesagem.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL(
      "CREATE TABLE IF NOT EXISTS `labels` (`id`INTEGER NOT NULL, `name` TEXT NOT NULL, `content` TEXT NOT NULL, PRIMARY KEY(`id`))"
    )
  }
}