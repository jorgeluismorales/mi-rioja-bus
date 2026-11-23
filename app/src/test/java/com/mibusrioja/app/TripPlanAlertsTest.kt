package com.mibusrioja.app

import com.mibusrioja.app.domain.model.Alert
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.RouteNode
import com.mibusrioja.app.domain.model.TripPlan
import com.mibusrioja.app.domain.model.TripStep
import com.mibusrioja.app.domain.model.TripStepType
import com.mibusrioja.app.domain.model.alertTargets
import com.mibusrioja.app.domain.model.hasActiveAlerts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlanAlertsTest {

    private val linea11 = BusLine("11", "Línea 11")
    private val linea22 = BusLine("22", "Línea 22")
    private val subida = RouteNode(-29.39, -66.82, esParada = true, codParada = "p-subida")
    private val transbordo = RouteNode(-29.42, -66.86, esParada = true, codParada = "p-transbordo")
    private val bajada = RouteNode(-29.43, -66.87, esParada = true, codParada = "p-bajada")

    private val directo = TripPlan(
        listOf(
            TripStep(TripStepType.CAMINAR, instruccion = "Caminá"),
            TripStep(TripStepType.TOMAR_COLECTIVO, linea11, subida, bajada, "Tomá la 11"),
            TripStep(TripStepType.LLEGAR, instruccion = "Llegaste")
        )
    )

    private val conTransbordo = TripPlan(
        listOf(
            TripStep(TripStepType.TOMAR_COLECTIVO, linea11, subida, transbordo, "Tomá la 11"),
            TripStep(TripStepType.TRANSBORDO, linea22, transbordo, transbordo, "Transbordo"),
            TripStep(TripStepType.TOMAR_COLECTIVO, linea22, transbordo, bajada, "Tomá la 22"),
            TripStep(TripStepType.LLEGAR, instruccion = "Llegaste")
        )
    )

    private fun alerta(linea: BusLine, parada: RouteNode, activa: Boolean = true) = Alert(
        codLinea = linea.codLinea,
        lineaDescripcion = linea.descripcion,
        codParada = parada.codParada!!,
        paradaDescripcion = "",
        distanciaMetros = 100,
        latParada = parada.lat,
        lngParada = parada.lng,
        activa = activa
    )

    @Test
    fun `un viaje directo lleva alerta solo en la bajada`() {
        assertEquals(listOf(linea11 to bajada), directo.alertTargets())
    }

    @Test
    fun `un viaje con transbordo lleva alerta en el transbordo y en la bajada`() {
        assertEquals(listOf(linea11 to transbordo, linea22 to bajada), conTransbordo.alertTargets())
    }

    @Test
    fun `el viaje figura con alertas mientras existan y estén activas`() {
        assertTrue(directo.hasActiveAlerts(listOf(alerta(linea11, bajada))))
    }

    @Test
    fun `al eliminar la alerta el viaje deja de figurar con alertas`() {
        assertFalse(directo.hasActiveAlerts(emptyList()))
        assertFalse(directo.hasActiveAlerts(listOf(alerta(linea22, bajada), alerta(linea11, subida))))
    }

    @Test
    fun `una alerta pausada no cuenta como activada`() {
        assertFalse(directo.hasActiveAlerts(listOf(alerta(linea11, bajada, activa = false))))
    }

    @Test
    fun `con transbordo hacen falta las dos alertas`() {
        assertFalse(conTransbordo.hasActiveAlerts(listOf(alerta(linea22, bajada))))
        assertTrue(conTransbordo.hasActiveAlerts(listOf(alerta(linea22, bajada), alerta(linea11, transbordo))))
    }

    @Test
    fun `un viaje sin paradas con codigo no figura con alertas`() {
        val sinCodigo = TripPlan(
            listOf(TripStep(TripStepType.TOMAR_COLECTIVO, linea11, subida, bajada.copy(codParada = null), "Tomá la 11"))
        )
        assertFalse(sinCodigo.hasActiveAlerts(listOf(alerta(linea11, bajada))))
    }
}
