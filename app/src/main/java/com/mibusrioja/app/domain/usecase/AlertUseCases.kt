package com.mibusrioja.app.domain.usecase

import com.mibusrioja.app.data.local.AlertEntity
import com.mibusrioja.app.data.repository.AlertRepository
import com.mibusrioja.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAlertsUseCase @Inject constructor(private val repo: AlertRepository) {
    operator fun invoke(): Flow<List<Alert>> = repo.getAllAlerts().map { entities ->
        entities.map { it.toDomain() }
    }
}

class CreateAlertUseCase @Inject constructor(private val repo: AlertRepository) {
    suspend operator fun invoke(alert: Alert): Long =
        repo.createAlert(alert.toEntity())
}

class DeleteAlertUseCase @Inject constructor(private val repo: AlertRepository) {
    suspend operator fun invoke(id: Long) = repo.deleteAlert(id)
}

class FindExistingAlertUseCase @Inject constructor(private val repo: AlertRepository) {
    suspend operator fun invoke(codLinea: String, codParada: String): AlertEntity? =
        repo.findExisting(codLinea, codParada)
}

class UpdateAlertDistanceUseCase @Inject constructor(private val repo: AlertRepository) {
    suspend operator fun invoke(id: Long, distanciaMetros: Int) =
        repo.setAlertDistance(id, distanciaMetros)
}

class ToggleAlertUseCase @Inject constructor(private val repo: AlertRepository) {
    suspend operator fun invoke(id: Long, activa: Boolean) = repo.setAlertActive(id, activa)
}

// ── Mappers ───────────────────────────────────────────────────────────────────
fun AlertEntity.toDomain() = Alert(
    id = id,
    codLinea = codLinea,
    lineaDescripcion = lineaDescripcion,
    codParada = codParada,
    paradaDescripcion = paradaDescripcion,
    distanciaMetros = distanciaMetros,
    latParada = latParada,
    lngParada = lngParada,
    activa = activa,
    isTransbordo = isTransbordo
)

fun Alert.toEntity() = AlertEntity(
    id = id,
    codLinea = codLinea,
    lineaDescripcion = lineaDescripcion,
    codParada = codParada,
    paradaDescripcion = paradaDescripcion,
    distanciaMetros = distanciaMetros,
    latParada = latParada,
    lngParada = lngParada,
    activa = activa,
    isTransbordo = isTransbordo
)
