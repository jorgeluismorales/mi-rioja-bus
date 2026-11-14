package com.mibusrioja.app.domain.geo

import com.mibusrioja.app.domain.model.BusPosition
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

sealed class BusMapItem {
    data class Single(val bus: BusPosition) : BusMapItem()
    data class Cluster(val lat: Double, val lng: Double, val buses: List<BusPosition>) : BusMapItem()
}

object BusClusterer {

    private fun mercatorY(lat: Double): Double =
        ln(tan(Math.PI / 4.0 + Math.toRadians(lat) / 2.0))

    /**
     * Agrupa colectivos por celda de grilla en proyección Web-Mercator.
     * Por debajo de zoom 17 los colectivos que caen en la misma celda se muestran como cluster.
     */
    fun cluster(buses: List<BusPosition>, zoom: Double, cellPx: Int = 64): List<BusMapItem> {
        if (buses.isEmpty()) return emptyList()
        if (zoom >= 17) return buses.map { BusMapItem.Single(it) }

        val worldPx = 256.0 * 2.0.pow(zoom)
        val cellDegLng = 360.0 * cellPx / worldPx
        val cellMercator = Math.PI * cellPx / worldPx

        val buckets = mutableMapOf<Long, MutableList<BusPosition>>()
        for (bus in buses) {
            val xCell = kotlin.math.floor(bus.lng / cellDegLng).toLong()
            val yCell = kotlin.math.floor(mercatorY(bus.lat) / cellMercator).toLong()
            val key = xCell * 100_000_000L + yCell
            buckets.getOrPut(key) { mutableListOf() }.add(bus)
        }

        return buckets.values.map { group ->
            if (group.size >= 2) {
                BusMapItem.Cluster(
                    lat = group.map { it.lat }.average(),
                    lng = group.map { it.lng }.average(),
                    buses = group
                )
            } else {
                BusMapItem.Single(group.first())
            }
        }
    }
}
