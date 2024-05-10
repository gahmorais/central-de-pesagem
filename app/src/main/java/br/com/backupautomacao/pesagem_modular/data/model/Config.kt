package br.com.backupautomacao.pesagem_modular.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracoes")
data class Config(
  @PrimaryKey
  @ColumnInfo(name = "nome")
  val name: String,
  @ColumnInfo(name = "valor")
  val value: String
)
