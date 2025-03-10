package br.com.backupautomacao.centraldepesagem.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import br.com.backupautomacao.centraldepesagem.db.model.Label

@Dao
interface LabelDao {
    @Insert
    suspend fun insert(label: Label)

    @Query("SELECT * FROM labels WHERE name LIKE :name LIMIT 1")
    suspend fun getLabelByName(name: String): Label
}