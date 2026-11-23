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
        val meters = route.locate(bus.lat, bus.lng, 262.0)!!
        assertFalse(route.hasPassed(meters, stopIdx))
        val remaining = route.metersToStop(meters, stopIdx)!!
        assertTrue("remaining=$remaining", remaining in 150.0..165.0)
    }

    @Test
    fun `bus just past the stop has passed`() {
        val bus = nodes[106] // ~162 m después, mismo rumbo
        val meters = route.locate(bus.lat, bus.lng, 262.0)!!
        assertTrue(route.hasPassed(meters, stopIdx))
        // En un circuito, lo que falta es casi una vuelta completa
        assertTrue(route.metersToStop(meters, stopIdx)!! > route.totalMeters * 0.9)
    }

    @Test
    fun `bus on the return leg next to the stop is not approaching`() {
        // Misma posición que la parada pero rumbo opuesto (82°): corresponde al tramo de vuelta
        val meters = route.locate(stop.lat, stop.lng, 82.0)!!
        assertTrue(route.hasPassed(meters, stopIdx))
    }

    @Test
    fun `bus at the stop heading along the route is located at the stop`() {
        val meters = route.locate(stop.lat, stop.lng, 262.0)!!
        assertFalse(route.hasPassed(meters, stopIdx))
        assertEquals(0.0, route.metersToStop(meters, stopIdx)!!, 1.0)
    }

    @Test
    fun `bus between two nodes is measured from where it is, not from the nearest node`() {
        val a = nodes[104]
        val meters = route.locate((a.lat + stop.lat) / 2, (a.lng + stop.lng) / 2, 262.0)!!
        val remaining = route.metersToStop(meters, stopIdx)!!
        assertTrue("remaining=$remaining", remaining in 75.0..83.0)
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

    // ── Línea 11: P005 (p004-tns, ida por Islas Decepción) y P093 (vuelta por Isla de los Estados, a 125 m) ──

    private val nodes11 = TestFixtures.parse<RutaLineaResponse>("rutaLinea_11.json").nodos.map {
        RouteNode(it.latitud, it.longitud, it.parada, it.codigoParada, it.descripcionParada)
    }
    private val route11 = RouteProgress(nodes11)
    private val p005 = nodes11.indexOfFirst { it.codParada == "p004-tns" }
    private val p093 = nodes11.indexOfFirst { it.codParada == "p093-tns" }

    private fun between(a: RouteNode, b: RouteNode, t: Double) =
        (a.lat + (b.lat - a.lat) * t) to (a.lng + (b.lng - a.lng) * t)

    @Test
    fun `bus on the parallel return street never approaches the outbound stop`() {
        // De P093 al nodo siguiente hay 311 m sin nodos intermedios: a mitad de tramo el nodo
        // más cercano es P005, en la calle paralela y en sentido contrario.
        for (step in 0..20) {
            val (lat, lng) = between(nodes11[p093], nodes11[p093 + 1], step / 20.0)
            val meters = route11.locate(lat, lng, 81.0)
            assertNotNull("step=$step", meters)
            assertTrue("step=$step meters=$meters", route11.hasPassed(meters!!, p005))
        }
    }

    @Test
    fun `bus on the outbound street approaches the stop`() {
        val (lat, lng) = between(nodes11[p005 - 1], nodes11[p005], 0.5)
        val meters = route11.locate(lat, lng, 262.0)!!
        assertFalse(route11.hasPassed(meters, p005))
        val remaining = route11.metersToStop(meters, p005)!!
        assertTrue("remaining=$remaining", remaining in 85.0..95.0)
    }
}
