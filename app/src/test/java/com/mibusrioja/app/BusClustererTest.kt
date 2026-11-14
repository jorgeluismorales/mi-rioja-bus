package com.mibusrioja.app

import com.mibusrioja.app.domain.geo.BusClusterer
import com.mibusrioja.app.domain.geo.BusMapItem
import com.mibusrioja.app.domain.model.BusPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusClustererTest {

    private fun bus(interno: String, lat: Double, lng: Double) =
        BusPosition(lat = lat, lng = lng, orientacion = 0f, interno = interno, tieneRampa = false)

    @Test
    fun `two buses 20m apart cluster at zoom 13`() {
        val items = BusClusterer.cluster(
            listOf(bus("A", -29.4134, -66.8560), bus("B", -29.4135, -66.8561)),
            zoom = 13.0
        )
        assertEquals(1, items.size)
        val cluster = items[0] as BusMapItem.Cluster
        assertEquals(2, cluster.buses.size)
    }

    @Test
    fun `same buses are singles at zoom 17`() {
        val items = BusClusterer.cluster(
            listOf(bus("A", -29.4134, -66.8560), bus("B", -29.4135, -66.8561)),
            zoom = 17.0
        )
        assertEquals(2, items.size)
        assertTrue(items.all { it is BusMapItem.Single })
    }

    @Test
    fun `buses 5km apart stay single at zoom 13`() {
        val items = BusClusterer.cluster(
            listOf(bus("A", -29.4134, -66.8560), bus("B", -29.4634, -66.8560)),
            zoom = 13.0
        )
        assertEquals(2, items.size)
        assertTrue(items.all { it is BusMapItem.Single })
    }

    @Test
    fun `empty list returns empty`() {
        assertTrue(BusClusterer.cluster(emptyList(), 13.0).isEmpty())
    }

    @Test
    fun `cluster centroid is the mean`() {
        val items = BusClusterer.cluster(
            listOf(bus("A", -29.4134, -66.8560), bus("B", -29.4136, -66.8562)),
            zoom = 13.0
        )
        val cluster = items[0] as BusMapItem.Cluster
        assertEquals(-29.4135, cluster.lat, 0.0001)
        assertEquals(-66.8561, cluster.lng, 0.0001)
    }
}
