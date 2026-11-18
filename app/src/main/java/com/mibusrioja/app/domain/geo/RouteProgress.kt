package com.mibusrioja.app.domain.geo

import com.mibusrioja.app.domain.model.RouteNode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
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

    /** Rumbo (0..360, 0 = norte) del tramo del recorrido en el nodo [i]. */
    fun segmentBearing(i: Int): Double {
        val (from, to) = if (i < nodes.lastIndex) nodes[i] to nodes[i + 1] else nodes[i - 1] to nodes[i]
        return bearing(from.lat, from.lng, to.lat, to.lng)
    }

    /**
     * Nodo del recorrido donde está el colectivo. Entre los nodos cercanos elige el que
     * mejor coincide con el rumbo; `null` si está fuera del recorrido.
     */
    fun locate(lat: Double, lng: Double, headingDeg: Double?): Int? {
        var best: Int? = null
        var bestScore = Double.MAX_VALUE
        var nearest: Int? = null
        var nearestDist = Double.MAX_VALUE
        for (i in nodes.indices) {
            val d = GeoUtils.distanceMeters(nodes[i].lat, nodes[i].lng, lat, lng)
            if (d < nearestDist) { nearestDist = d; nearest = i }
            if (d > CANDIDATE_RADIUS_M) continue
            val penalty = headingDeg?.let { angleDiff(it, segmentBearing(i)) * HEADING_PENALTY_M_PER_DEG } ?: 0.0
            val score = d + penalty
            if (score < bestScore) { bestScore = score; best = i }
        }
        return best ?: nearest?.takeIf { nearestDist <= OFF_ROUTE_M }
    }

    /**
     * Metros que le faltan al colectivo (en [busIndex]) para llegar a la parada ([stopIndex])
     * siguiendo el recorrido. Si ya la pasó: en un circuito, lo que falta para la próxima
     * vuelta; si no, `null`.
     */
    fun metersToStop(busIndex: Int, stopIndex: Int): Double? {
        val ahead = cumulative[stopIndex] - cumulative[busIndex]
        return when {
            ahead >= 0 -> ahead
            isLoop -> totalMeters + ahead
            else -> null
        }
    }

    /** `true` si el colectivo ya pasó la parada en esta vuelta. */
    fun hasPassed(busIndex: Int, stopIndex: Int): Boolean = busIndex > stopIndex

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
