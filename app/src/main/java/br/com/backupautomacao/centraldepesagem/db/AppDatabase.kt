package br.com.backupautomacao.centraldepesagem.db

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.backupautomacao.centraldepesagem.db.dao.LabelDao
import br.com.backupautomacao.centraldepesagem.db.dao.OperationDao
import br.com.backupautomacao.centraldepesagem.db.model.Label
import br.com.backupautomacao.centraldepesagem.db.model.Operation

@Database(
    entities = [Operation::class, Label::class],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun operationDao(): OperationDao
    abstract fun labelDao(): LabelDao
}
