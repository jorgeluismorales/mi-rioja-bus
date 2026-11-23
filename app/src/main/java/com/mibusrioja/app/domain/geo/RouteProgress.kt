package com.mibusrioja.app.domain.geo

import com.mibusrioja.app.domain.model.RouteNode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Proyecta posiciones sobre el recorrido de una línea para saber si un colectivo
 * todavía no llegó a una parada (se acerca) o ya la pasó.
 *
 * Los recorridos son circuitos que suelen ir y volver por las mismas calles, por lo
 * que la posición sola es ambigua: se usa el rumbo ([orientacion]) del colectivo para
 * elegir el tramo del recorrido que coincide con su sentido de marcha.
 */
class RouteProgress(private val nodes: List<RouteNode>) {

    private companion object {
        const val CANDIDATE_RADIUS_M = 80.0
        const val OFF_ROUTE_M = 150.0
        const val LOOP_CLOSE_M = 200.0
        /** Un colectivo hasta esta distancia después de la parada se considera detenido en ella. */
        const val AT_STOP_TOLERANCE_M = 20.0
        /** Penalización en metros equivalentes por cada grado de diferencia de rumbo. */
        const val HEADING_PENALTY_M_PER_DEG = 1.0
        /** Puntos cuyo puntaje difiere menos que esto son igual de probables. */
        const val AMBIGUITY_MARGIN_M = 45.0
        /** Puntos a menos de esta distancia sobre el recorrido son el mismo lugar. */
        const val SAME_PLACE_M = 300.0
        const val BACKTRACK_TOLERANCE_M = 100.0
    }

    private class Candidate(val meters: Double, val score: Double)

    /** Distancia acumulada a lo largo del recorrido hasta cada nodo. */
    private val cumulative = DoubleArray(nodes.size).also { acc ->
        for (i in 1 until nodes.size) {
            acc[i] = acc[i - 1] + GeoUtils.distanceMeters(
                nodes[i - 1].lat, nodes[i - 1].lng, nodes[i].lat, nodes[i].lng
            )
        }
    }

    val totalMeters: Double get() = cumulative.lastOrNull() ?: 0.0

    /** El recorrido es un circuito cerrado (fin cerca del inicio). */
    val isLoop: Boolean = nodes.size >= 2 &&
        GeoUtils.distanceMeters(nodes.first().lat, nodes.first().lng, nodes.last().lat, nodes.last().lng) <= LOOP_CLOSE_M

    /** Índice del nodo de la parada: por código, y si no, la parada más cercana a (lat, lng). */
    fun stopIndex(codParada: String?, lat: Double, lng: Double): Int? {
        if (!codParada.isNullOrBlank()) {
            nodes.indexOfFirst { it.esParada && it.codParada == codParada }.takeIf { it >= 0 }?.let { return it }
        }
        return nodes.indices
            .filter { nodes[it].esParada }
            .minByOrNull { GeoUtils.distanceMeters(nodes[it].lat, nodes[it].lng, lat, lng) }
            ?.takeIf { GeoUtils.distanceMeters(nodes[it].lat, nodes[it].lng, lat, lng) <= OFF_ROUTE_M }
    }

    /** Rumbo (0..360, 0 = norte) del tramo que sale del nodo [i]. */
    private fun segmentBearing(i: Int): Double =
        bearing(nodes[i].lat, nodes[i].lng, nodes[i + 1].lat, nodes[i + 1].lng)

    /**
     * Puntos del recorrido (metros desde el inicio) donde puede estar el colectivo, el más
     * probable primero; vacío si está fuera del recorrido.
     *
     * Se proyecta la posición sobre cada **tramo** (no sobre los nodos, que pueden estar a más
     * de 300 m entre sí) y se puntúa cada tramo cercano por distancia y diferencia de rumbo.
     * Hay más de un punto cuando el recorrido pasa más de una vez por el mismo lugar con el
     * mismo rumbo (p. ej. la entrada a Las Cloacas de la línea 10, en la ida y en la vuelta):
     * ahí la posición y el rumbo no alcanzan y hace falta saber de dónde viene ([BusTracker]).
     */
    fun candidates(lat: Double, lng: Double, headingDeg: Double?): List<Double> {
        val near = ArrayList<Candidate>()
        val far = ArrayList<Candidate>()
        val metersPerDegLat = Math.toRadians(1.0) * GeoUtils.EARTH_RADIUS_M
        val metersPerDegLng = metersPerDegLat * cos(Math.toRadians(lat))
        for (i in 0 until nodes.lastIndex) {
            val length = cumulative[i + 1] - cumulative[i]
            if (length <= 0.0) continue
            // Coordenadas planas en metros con origen en el colectivo
            val ax = (nodes[i].lng - lng) * metersPerDegLng
            val ay = (nodes[i].lat - lat) * metersPerDegLat
            val dx = (nodes[i + 1].lng - nodes[i].lng) * metersPerDegLng
            val dy = (nodes[i + 1].lat - nodes[i].lat) * metersPerDegLat
            val t = (-(ax * dx + ay * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
            val d = hypot(ax + t * dx, ay + t * dy)
            if (d > OFF_ROUTE_M) continue
            val penalty = headingDeg?.let { angleDiff(it, segmentBearing(i)) * HEADING_PENALTY_M_PER_DEG } ?: 0.0
            val candidate = Candidate(cumulative[i] + t * length, d + penalty)
            if (d <= CANDIDATE_RADIUS_M) near += candidate else far += candidate
        }
        val pool = near.ifEmpty { far }.sortedBy { it.score }
        val best = pool.firstOrNull() ?: return emptyList()
        val result = ArrayList<Double>()
        for (c in pool) {
            if (c.score > best.score + AMBIGUITY_MARGIN_M) break
            // Tramos vecinos dan el mismo lugar: queda solo el de mejor puntaje
            if (result.none { routeDistance(it, c.meters) < SAME_PLACE_M }) result += c.meters
        }
        return result
    }

    /** El punto más probable de [candidates], sin tener en cuenta de dónde viene el colectivo. */
    fun locate(lat: Double, lng: Double, headingDeg: Double?): Double? =
        candidates(lat, lng, headingDeg).firstOrNull()

    /**
     * Metros que hay que avanzar por el recorrido para ir de [fromMeters] a [toMeters]. Un
     * retroceso corto cuenta como 0 (ruido del GPS); uno mayor es una vuelta más en un
     * circuito, o imposible si no lo es.
     */
    fun metersAhead(fromMeters: Double, toMeters: Double): Double {
        val delta = toMeters - fromMeters
        return when {
            delta >= -BACKTRACK_TOLERANCE_M -> delta.coerceAtLeast(0.0)
            isLoop -> totalMeters + delta
            else -> Double.POSITIVE_INFINITY
        }
    }

    private fun routeDistance(a: Double, b: Double): Double {
        val d = abs(a - b)
        return if (isLoop) minOf(d, totalMeters - d) else d
    }

    /**
     * Metros que le faltan al colectivo (a [busMeters] del inicio) para llegar a la parada
     * ([stopIndex]) siguiendo el recorrido. Si ya la pasó: en un circuito, lo que falta para
     * la próxima vuelta; si no, `null`.
     */
    fun metersToStop(busMeters: Double, stopIndex: Int): Double? {
        val ahead = cumulative[stopIndex] - busMeters
        return when {
            ahead >= -AT_STOP_TOLERANCE_M -> ahead.coerceAtLeast(0.0)
            isLoop -> totalMeters + ahead
            else -> null
        }
    }

    /** `true` si el colectivo ya pasó la parada en esta vuelta. */
    fun hasPassed(busMeters: Double, stopIndex: Int): Boolean =
        busMeters > cumulative[stopIndex] + AT_STOP_TOLERANCE_M

    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(((a - b) % 360 + 360) % 360)
        return if (d > 180) 360 - d else d
    }

    private fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val f1 = Math.toRadians(lat1)
        val f2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lng2 - lng1)
        val y = sin(dl) * cos(f2)
        val x = cos(f1) * sin(f2) - sin(f1) * cos(f2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }
}
