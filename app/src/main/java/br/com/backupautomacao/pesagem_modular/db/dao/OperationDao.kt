package br.com.backupautomacao.pesagem_modular.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import br.com.backupautomacao.pesagem_modular.db.model.Operation
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {

  @Query("SELECT * FROM operacoes")
  fun getAll() : Flow<List<Operation>>

  @Insert
  suspend fun save(operation: Operation)
}