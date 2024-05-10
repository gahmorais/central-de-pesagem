package br.com.backupautomacao.pesagem_modular.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.backupautomacao.pesagem_modular.data.model.Config

@Dao
interface ConfigDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setConfig(config: Config)

  @Query("SELECT * FROM configuracoes WHERE nome = :key")
  suspend fun getConfig(key: String): Config?
}