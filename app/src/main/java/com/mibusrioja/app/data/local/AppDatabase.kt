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

    // Si hay varias para la misma parada, prioriza la activa: así no se reactiva una pausada duplicada.
    @Query("SELECT * FROM alerts WHERE codLinea = :codLinea AND codParada = :codParada ORDER BY activa DESC LIMIT 1")
    suspend fun findByLineAndStop(codLinea: String, codParada: String): AlertEntity?
}

// ── Frequent destinations ─────────────────────────────────────────────────────
@Entity(tableName = "frequent_destinations")
data class FrequentDestinationEntity(
    @PrimaryKey val key: String,
    val nombre: String,
    val detalle: String,
    val lat: Double,
    val lng: Double,
    val vecesUsado: Int = 1,
    val ultimoUso: Long = System.currentTimeMillis()
)

@Dao
interface FrequentDestinationDao {
    @Query("SELECT * FROM frequent_destinations ORDER BY vecesUsado DESC, ultimoUso DESC LIMIT :limit")
    fun getTop(limit: Int = 6): Flow<List<FrequentDestinationEntity>>

    @Query("SELECT * FROM frequent_destinations WHERE key = :key LIMIT 1")
    suspend fun find(key: String): FrequentDestinationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: FrequentDestinationEntity)

    @Query("DELETE FROM frequent_destinations WHERE key = :key")
    suspend fun delete(key: String)

    /** Mueve una entrada a otra clave; si ya había una en el destino, suma los usos y conserva el último uso. */
    @Transaction
    suspend fun relocate(oldKey: String, newKey: String, lat: Double, lng: Double) {
        val old = find(oldKey) ?: return
        val existing = find(newKey)
        upsert(
            old.copy(
                key = newKey,
                lat = lat,
                lng = lng,
                vecesUsado = old.vecesUsado + (existing?.vecesUsado ?: 0),
                ultimoUso = maxOf(old.ultimoUso, existing?.ultimoUso ?: 0L)
            )
        )
        delete(oldKey)
    }
}

// ── Database ──────────────────────────────────────────────────────────────────
@Database(
    entities = [AlertEntity::class, FrequentDestinationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun frequentDestinationDao(): FrequentDestinationDao
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS frequent_destinations (
                key TEXT NOT NULL PRIMARY KEY,
                nombre TEXT NOT NULL,
                detalle TEXT NOT NULL,
                lat REAL NOT NULL,
                lng REAL NOT NULL,
                vecesUsado INTEGER NOT NULL,
                ultimoUso INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
