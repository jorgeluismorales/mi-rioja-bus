package com.mibusrioja.app

import com.mibusrioja.app.data.api.model.RutaLineaResponse
import com.mibusrioja.app.domain.geo.RouteProgress
import com.mibusrioja.app.domain.model.RouteNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Usa el recorrido real de la línea 10 (fixture). Parada p021-te = nodo 105; la vuelta pasa por el nodo 256. */
class RouteProgressTest {

    private val nodes = TestFixtures.parse<RutaLineaResponse>("rutaLinea_10.json").nodos.map {
        RouteNode(it.latitud, it.longitud, it.parada, it.codigoParada, it.descripcionParada)
    }
    private val route = RouteProgress(nodes)
    private val stop = nodes[105]
    private val stopIdx = route.stopIndex("p021-te", stop.lat, stop.lng)!!

    @Test
    fun `stop is resolved by code`() = assertEquals(105, stopIdx)

    @Test
    fun `route 10 is a loop`() = assertTrue(route.isLoop)

    @Test
    fun `bus before the stop heading towards it is approaching`() {
        val bus = nodes[104] // ~157 m antes, rumbo 262°
        val idx = route.locate(bus.lat, bus.lng, 262.0)!!
        assertFalse(route.hasPassed(idx, stopIdx))
        val remaining = route.metersToStop(idx, stopIdx)!!
        assertTrue("remaining=$remaining", remaining in 150.0..165.0)
    }

    @Test
    fun `bus just past the stop has passed`() {
        val bus = nodes[106] // ~162 m después, mismo rumbo
        val idx = route.locate(bus.lat, bus.lng, 262.0)!!
        assertTrue(route.hasPassed(idx, stopIdx))
        // En un circuito, lo que falta es casi una vuelta completa
        assertTrue(route.metersToStop(idx, stopIdx)!! > route.totalMeters * 0.9)
    }

    @Test
    fun `bus on the return leg next to the stop is not approaching`() {
        // Misma posición que la parada pero rumbo opuesto (82°): corresponde al tramo de vuelta (nodo 256)
        val idx = route.locate(stop.lat, stop.lng, 82.0)!!
        assertEquals(256, idx)
        assertTrue(route.hasPassed(idx, stopIdx))
    }

    @Test
    fun `bus at the stop heading along the route is located at the stop`() {
        assertEquals(105, route.locate(stop.lat, stop.lng, 262.0))
    }

    @Test
    fun `bus far from the route is not located`() {
        assertNull(route.locate(-29.30, -66.60, 0.0))
    }

    @Test
    fun `unknown stop code falls back to nearest stop`() {
        assertNotNull(route.stopIndex("no-existe", stop.lat + 0.0002, stop.lng))
        assertEquals(105, route.stopIndex(null, stop.lat, stop.lng))
    }
}
