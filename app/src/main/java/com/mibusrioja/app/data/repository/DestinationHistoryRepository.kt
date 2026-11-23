package com.mibusrioja.app.data.repository

import com.mibusrioja.app.data.local.FrequentDestinationDao
import com.mibusrioja.app.data.local.FrequentDestinationEntity
import com.mibusrioja.app.domain.model.FrequentDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DestinationHistoryRepository @Inject constructor(
    private val dao: FrequentDestinationDao
) {
    val frequent: Flow<List<FrequentDestination>> = dao.getTop().map { entities ->
        entities.map {
            FrequentDestination(
                key = it.key,
                nombre = it.nombre,
                detalle = it.detalle,
                lat = it.lat,
                lng = it.lng,
                vecesUsado = it.vecesUsado
            )
        }
    }

    suspend fun registerVisit(nombre: String, detalle: String, lat: Double, lng: Double) {
        val key = keyFor(lat, lng)
        val existing = dao.find(key)
        dao.upsert(
            FrequentDestinationEntity(
                key = key,
                nombre = nombre,
                detalle = detalle,
                lat = lat,
                lng = lng,
                vecesUsado = (existing?.vecesUsado ?: 0) + 1,
                ultimoUso = System.currentTimeMillis()
            )
        )
    }

    /** Mueve un destino guardado a coordenadas corregidas, conservando su historial de uso. */
    suspend fun relocate(key: String, lat: Double, lng: Double) {
        val newKey = keyFor(lat, lng)
        if (newKey == key) return
        val old = dao.find(key) ?: return
        val merged = dao.find(newKey)
        dao.upsert(
            old.copy(
                key = newKey,
                lat = lat,
                lng = lng,
                vecesUsado = old.vecesUsado + (merged?.vecesUsado ?: 0)
            )
        )
        dao.delete(key)
    }

    private fun keyFor(lat: Double, lng: Double) = "%.4f,%.4f".format(Locale.US, lat, lng)

    suspend fun remove(key: String) {
        dao.delete(key)
    }
}
