package com.mibusrioja.app.data.repository

import com.mibusrioja.app.data.local.AlertDao
import com.mibusrioja.app.data.local.AlertEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val alertDao: AlertDao
) {
    fun getAllAlerts(): Flow<List<AlertEntity>> = alertDao.getAllAlerts()

    suspend fun getActiveAlerts(): List<AlertEntity> = alertDao.getActiveAlerts()

    suspend fun createAlert(alert: AlertEntity): Long = alertDao.insertAlert(alert)

    suspend fun deleteAlert(id: Long) = alertDao.deleteById(id)

    suspend fun setAlertActive(id: Long, activa: Boolean) = alertDao.setActive(id, activa)

    suspend fun updateAlert(alert: AlertEntity) = alertDao.updateAlert(alert)
}
