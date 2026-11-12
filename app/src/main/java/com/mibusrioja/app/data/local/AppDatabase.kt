package com.mibusrioja.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entity ────────────────────────────────────────────────────────────────────
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codLinea: String,
    val lineaDescripcion: String,
    val codParada: String,
    val paradaDescripcion: String,
    val distanciaMetros: Int,           // umbral de alerta en metros
    val latParada: Double,
    val lngParada: Double,
    val activa: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isTransbordo: Boolean = false   // true si fue auto-creada por planificador
)

// ── DAO ───────────────────────────────────────────────────────────────────────
@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE activa = 1")
    suspend fun getActiveAlerts(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE alerts SET activa = :activa WHERE id = :id")
    suspend fun setActive(id: Long, activa: Boolean)
}

// ── Database ──────────────────────────────────────────────────────────────────
@Database(entities = [AlertEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
}
