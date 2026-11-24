package com.mibusrioja.app.domain.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    const val EARTH_RADIUS_M = 6_371_008.8

    /** Distancia haversine en metros entre dos coordenadas lat/lng. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }
}
