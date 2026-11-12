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
    val estaFuera: Boolean
)

// ── Configuración ─────────────────────────────────────────────────────────────
data class AppConfig(
    val latCenter: Double = -29.4134003,
    val lngCenter: Double = -66.8560292,
    val urlPrincipalMapas: String = "https://mapmoblrj.red-bus.com.ar/tiles/{z}/{x}/{y}.png",
    val urlSecundariaMapas: String = "https://b.tile.openstreetmap.org/{z}/{x}/{y}.png",
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
    val horario: String? = null
)

// ── Saldo ─────────────────────────────────────────────────────────────────────
data class Saldo(
    val nroTarjeta: String,
    val saldo: Double,
    val tipo: String,
    val movimientos: List<Movimiento>
)

data class Movimiento(
    val fecha: String,
    val monto: Double,
    val tipo: String,
    val descripcion: String? = null,
    val linea: String? = null
)

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
data class TripPlan(
    val pasos: List<TripStep>,
    val duracionEstimadaMin: Int = 0
)

data class TripStep(
    val tipo: TripStepType,
    val linea: BusLine? = null,
    val paradaOrigen: RouteNode? = null,
    val paradaDestino: RouteNode? = null,
    val instruccion: String,
    val distanciaMetros: Int = 0
)

enum class TripStepType { CAMINAR, TOMAR_COLECTIVO, TRANSBORDO, LLEGAR }

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
