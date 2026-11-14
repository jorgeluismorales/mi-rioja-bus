package com.mibusrioja.app

import com.mibusrioja.app.domain.geo.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun `distancia plaza 25 de mayo a unlar entre 2_5 y 3_2 km`() {
        val d = GeoUtils.distanceMeters(
            -29.4134003, -66.8560292,
            -29.434720, -66.871530
        )
        assertTrue("expected ~2.8km, got ${d}m", d in 2500.0..3200.0)
    }

    @Test
    fun `mismo punto da cero`() {
        assertEquals(0.0, GeoUtils.distanceMeters(-29.4, -66.8, -29.4, -66.8), 0.001)
    }
}
