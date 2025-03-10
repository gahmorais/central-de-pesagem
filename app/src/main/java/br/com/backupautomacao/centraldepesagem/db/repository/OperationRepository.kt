package br.com.backupautomacao.centraldepesagem.db.repository

import androidx.annotation.WorkerThread
import androidx.room.Delete
import br.com.backupautomacao.centraldepesagem.db.dao.OperationDao
import br.com.backupautomacao.centraldepesagem.db.model.Operation
import kotlinx.coroutines.flow.Flow

class OperationRepository(private val operationDao: OperationDao) {

    val allOperations: Flow<List<Operation>> = operationDao.getAll()

    @WorkerThread
    suspend fun save(operation: Operation) {
        operationDao.save(operation)
    }

    @Delete
    fun deleteAll() {
    }

}