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
    }

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
     * Metros recorridos desde el inicio hasta el punto del recorrido donde está el colectivo;
     * `null` si está fuera del recorrido.
     *
     * Se proyecta la posición sobre cada **tramo** (no sobre los nodos, que pueden estar a más
     * de 300 m entre sí) y, entre los tramos cercanos, se elige el que mejor coincide con el rumbo.
     */
    fun locate(lat: Double, lng: Double, headingDeg: Double?): Double? {
        var best: Double? = null
        var bestScore = Double.MAX_VALUE
        var fallback: Double? = null
        var fallbackScore = Double.MAX_VALUE
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
            val score = d + penalty
            val meters = cumulative[i] + t * length
            if (d <= CANDIDATE_RADIUS_M) {
                if (score < bestScore) { bestScore = score; best = meters }
            } else if (score < fallbackScore) {
                fallbackScore = score; fallback = meters
            }
        }
        return best ?: fallback
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
