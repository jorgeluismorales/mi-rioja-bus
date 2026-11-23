package com.mibusrioja.app.domain.geo

/**
 * Ubica los colectivos de una línea sobre su recorrido recordando por dónde venía cada uno.
 *
 * Donde el recorrido pasa más de una vez por el mismo lugar con el mismo rumbo, la posición y
 * el rumbo no dicen en cuál de las pasadas está el colectivo: se elige la primera que tiene
 * por delante desde el último punto donde se lo vio.
 */
class BusTracker(private val route: RouteProgress) {

    private companion object {
        /** Pasado este tiempo sin verlo, el último punto conocido ya no sirve de referencia. */
        const val HISTORY_TTL_MS = 5 * 60_000L
    }

    private class Sighting(val meters: Double, val atMs: Long)

    private val lastSeen = mutableMapOf<String, Sighting>()

    /**
     * Puntos del recorrido (metros desde el inicio) donde puede estar el colectivo [interno].
     * Es uno solo, salvo que esté en un lugar ambiguo y todavía no se sepa de dónde viene;
     * vacío si está fuera del recorrido.
     */
    fun locate(
        interno: String,
        lat: Double,
        lng: Double,
        headingDeg: Double?,
        nowMs: Long = System.currentTimeMillis()
    ): List<Double> {
        val candidates = route.candidates(lat, lng, headingDeg)
        val previous = lastSeen[interno]?.takeIf { nowMs - it.atMs <= HISTORY_TTL_MS }?.meters
        val meters = when {
            candidates.size == 1 -> candidates.first()
            previous != null -> candidates.minByOrNull { route.metersAhead(previous, it) }
            else -> null
        } ?: return candidates
        if (interno.isNotBlank()) lastSeen[interno] = Sighting(meters, nowMs)
        return listOf(meters)
    }

    /** Olvida los colectivos que ya no están en servicio. */
    fun retainBuses(internos: Set<String>) {
        lastSeen.keys.retainAll(internos)
    }
}
