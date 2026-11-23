package com.mibusrioja.app.domain.model

// ── Líneas ────────────────────────────────────────────────────────────────────
data class BusLine(
    val codLinea: String,
    val descripcion: String,
    val grupo: String = ""
)

data class LineGroup(
    val nombre: String,
    val lineas: List<BusLine>,
    val subGrupos: List<LineGroup> = emptyList()
)

// ── Ruta / Paradas ────────────────────────────────────────────────────────────
data class RouteNode(
    val lat: Double,
    val lng: Double,
    val esParada: Boolean,
    val codParada: String? = null,
    val nombreParada: String? = null
)

// ── Buses en tiempo real ──────────────────────────────────────────────────────
data class BusPosition(
    val lat: Double,
    val lng: Double,
    val orientacion: Float,
    val interno: String,
    val tieneRampa: Boolean,
    val proximaParada: String? = null
)

// ── Configuración ─────────────────────────────────────────────────────────────
data class AppConfig(
    val latCenter: Double = -29.4134003,
    val lngCenter: Double = -66.8560292,
    val urlPrincipalMapas: String = "https://mapmoblrj.red-bus.com.ar/tiles/{z}/{x}/{y}.png",
    val urlSecundariaMapas: String = "",
    val tarifas: List<TarifaItem> = emptyList()
)

data class TarifaItem(
    val categoria: String,
    val precio: String
)

// ── Puntos de venta ───────────────────────────────────────────────────────────
enum class TipoPuntoVenta { ATM, COMUN, ABONO }

data class PuntoVenta(
    val nombre: String,
    val direccion: String,
    val lat: Double,
    val lng: Double,
    val tipo: TipoPuntoVenta,
    val descripcionTipo: String
)

// ── Saldo ─────────────────────────────────────────────────────────────────────
data class Saldo(
    val nroTarjeta: String,
    val saldo: Double,
    val tipo: String,
    val estado: String? = null,
    val fechaSaldo: String? = null,
    val etiqueta: String? = null
)

class SaldoException(val code: Int, message: String) : Exception(message)

// ── Alertas ───────────────────────────────────────────────────────────────────
data class Alert(
    val id: Long = 0,
    val codLinea: String,
    val lineaDescripcion: String,
    val codParada: String,
    val paradaDescripcion: String,
    val distanciaMetros: Int,
    val latParada: Double,
    val lngParada: Double,
    val activa: Boolean = true,
    val isTransbordo: Boolean = false
)

// ── Planificador ──────────────────────────────────────────────────────────────
class NoRouteException(message: String) : Exception(message)

data class FrequentDestination(
    val key: String,
    val nombre: String,
    val detalle: String,
    val lat: Double,
    val lng: Double,
    val vecesUsado: Int
)

data class PlaceSuggestion(
    val nombre: String,
    val detalle: String,
    val lat: Double,
    val lng: Double
)

data class TripPlan(
    val pasos: List<TripStep>,
    val origen: Pair<Double, Double>? = null,
    val destino: Pair<Double, Double>? = null,
    val duracionEstimadaMin: Int = 0
)

data class TripStep(
    val tipo: TripStepType,
    val linea: BusLine? = null,
    val paradaOrigen: RouteNode? = null,
    val paradaDestino: RouteNode? = null,
    val instruccion: String,
    val distanciaMetros: Int = 0,
    val recorrido: List<RouteNode> = emptyList()
)

enum class TripStepType { CAMINAR, TOMAR_COLECTIVO, TRANSBORDO, LLEGAR }

/** Paradas del viaje que llevan alerta de proximidad: cada transbordo y la bajada final. */
fun TripPlan.alertTargets(): List<Pair<BusLine, RouteNode>> {
    val targets = mutableListOf<Pair<BusLine, RouteNode>>()
    for (i in pasos.indices) {
        val step = pasos[i]
        if (step.tipo == TripStepType.TRANSBORDO) {
            val prevBus = pasos.take(i).lastOrNull { it.tipo == TripStepType.TOMAR_COLECTIVO }
            val line = prevBus?.linea ?: step.linea
            val stop = step.paradaOrigen
            if (line != null && stop?.codParada != null) targets.add(line to stop)
        }
    }
    pasos.lastOrNull { it.tipo == TripStepType.TOMAR_COLECTIVO }?.let { last ->
        val line = last.linea
        val stop = last.paradaDestino
        if (line != null && stop?.codParada != null) targets.add(line to stop)
    }
    return targets
}

/** True si cada parada con alerta del viaje tiene hoy una alerta activa. */
fun TripPlan.hasActiveAlerts(alerts: List<Alert>): Boolean {
    val targets = alertTargets()
    return targets.isNotEmpty() && targets.all { (line, stop) ->
        alerts.any { it.activa && it.codLinea == line.codLinea && it.codParada == stop.codParada }
    }
}

// ── Tiempo estimado ───────────────────────────────────────────────────────────
data class TiempoEstimado(
    val segundosFaltantes: Int?,
    val interno: String?
) {
    val minutosFaltantes: Int? get() = segundosFaltantes?.let { it / 60 }

    fun formatearTiempo(): String = when {
        segundosFaltantes == null -> "Sin datos"
        segundosFaltantes <= 60 -> "Menos de 1 min"
        minutosFaltantes == 1 -> "1 minuto"
        else -> "$minutosFaltantes minutos"
    }
}
