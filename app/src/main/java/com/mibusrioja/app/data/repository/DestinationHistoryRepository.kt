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
        val key = "%.4f,%.4f".format(Locale.US, lat, lng)
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

    suspend fun remove(key: String) {
        dao.delete(key)
    }
}
