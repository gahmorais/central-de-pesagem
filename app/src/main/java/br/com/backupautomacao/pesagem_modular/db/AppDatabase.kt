package br.com.backupautomacao.pesagem_modular.db

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.backupautomacao.pesagem_modular.db.dao.LabelDao
import br.com.backupautomacao.pesagem_modular.db.dao.OperationDao
import br.com.backupautomacao.pesagem_modular.db.model.Label
import br.com.backupautomacao.pesagem_modular.db.model.Operation

@Database(
    entities = [Operation::class, Label::class],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun operationDao(): OperationDao
    abstract fun labelDao(): LabelDao
}
