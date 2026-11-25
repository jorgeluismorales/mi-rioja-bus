package com.mibusrioja.app.data.repository

import com.mibusrioja.app.data.api.BrokenAccents
import com.mibusrioja.app.data.local.AlertDao
import com.mibusrioja.app.data.local.AlertEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val alertDao: AlertDao
) {
    fun getAllAlerts(): Flow<List<AlertEntity>> =
        alertDao.getAllAlerts().map { alerts -> alerts.map { it.withRepairedNames() } }

    suspend fun getActiveAlerts(): List<AlertEntity> =
        alertDao.getActiveAlerts().map { it.withRepairedNames() }

    suspend fun createAlert(alert: AlertEntity): Long = alertDao.insertAlert(alert)

    suspend fun deleteAlert(id: Long) = alertDao.deleteById(id)

    suspend fun setAlertActive(id: Long, activa: Boolean) = alertDao.setActive(id, activa)

    suspend fun setAlertDistance(id: Long, distanciaMetros: Int) =
        alertDao.setDistance(id, distanciaMetros)

    suspend fun updateAlert(alert: AlertEntity) = alertDao.updateAlert(alert)

    suspend fun findExisting(codLinea: String, codParada: String): AlertEntity? =
        alertDao.findByLineAndStop(codLinea, codParada)
}

/** Alertas guardadas antes de reparar los acentos de la API. */
private fun AlertEntity.withRepairedNames() = copy(
    lineaDescripcion = BrokenAccents.repair(lineaDescripcion),
    paradaDescripcion = BrokenAccents.repair(paradaDescripcion)
)
