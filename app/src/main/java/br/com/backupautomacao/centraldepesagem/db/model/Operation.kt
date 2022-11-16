package br.com.backupautomacao.centraldepesagem.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operacoes")
data class Operation(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Int = 0,
  @ColumnInfo(name = "username")
  val username: String,
  @ColumnInfo(name = "descricao")
  val description: String,
  @ColumnInfo(name = "peso")
  val weight: Double,
  @ColumnInfo(name = "preco_por_kg")
  val pricePerKilogram: Double,
  @ColumnInfo(name = "preco_total")
  val totalPrice: Double,
  @ColumnInfo(name = "data")
  val date: Long = System.currentTimeMillis()
)
