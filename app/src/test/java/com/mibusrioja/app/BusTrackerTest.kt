package com.mibusrioja.app

import com.mibusrioja.app.data.api.model.RutaLineaResponse
import com.mibusrioja.app.domain.geo.BusTracker
import com.mibusrioja.app.domain.geo.GeoUtils
import com.mibusrioja.app.domain.geo.RouteProgress
import com.mibusrioja.app.domain.model.RouteNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Recorridos reales (fixtures). En la línea 10 la entrada a Las Cloacas se recorre dos veces,
 * por la misma calle y en el mismo sentido: en la ida (nodos 20–51, parada p003-te) y en la
 * vuelta (nodos 305–334, parada p066-te), a 23 km una de otra.
 */
class BusTrackerTest {

    private class Sample(val lat: Double, val lng: Double, val heading: Double, val meters: Double)

    private class Line(fixture: String) {
        val nodes = TestFixtures.parse<RutaLineaResponse>(fixture).nodos.map {
            RouteNode(it.latitud, it.longitud, it.parada, it.codigoParada, it.descripcionParada)
        }
        val route = RouteProgress(nodes)
        val cumulative = DoubleArray(nodes.size).also { acc ->
            for (i in 1 until nodes.size) {
                acc[i] = acc[i - 1] +
                    GeoUtils.distanceMeters(nodes[i - 1].lat, nodes[i - 1].lng, nodes[i].lat, nodes[i].lng)
            }
        }

        fun stop(cod: String) = nodes.indexOfFirst { it.codParada == cod }

        /** Posiciones de un colectivo que va del nodo [from] al nodo [to], una cada [stepM] metros. */
        fun drive(from: Int, to: Int, stepM: Double = 25.0): List<Sample> {
            val out = ArrayList<Sample>()
            for (i in from until to) {
                val a = nodes[i]
                val b = nodes[i + 1]
                val length = cumulative[i + 1] - cumulative[i]
                if (length <= 0.0) continue
                val heading = bearing(a, b)
                val steps = maxOf(1, (length / stepM).toInt())
                for (j in 0 until steps) {
                    val t = j.toDouble() / steps
                    out += Sample(
                        a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t,
                        heading, cumulative[i] + t * length
                    )
                }
            }
            return out
        }

        fun error(located: Double, expected: Double): Double {
            val d = abs(located - expected)
            return minOf(d, route.totalMeters - d)
        }
    }

    private val line10 = Line("rutaLinea_10.json")

    /** Desplaza la posición hasta [maxM] metros y el rumbo hasta [maxDeg] grados, como un GPS real. */
    private fun Sample.noisy(random: Random, maxM: Double = 15.0, maxDeg: Double = 20.0): Sample {
        val north = random.nextDouble(-maxM, maxM)
        val east = random.nextDouble(-maxM, maxM)
        return Sample(
            lat + north / 111_195.0,
            lng + east / (111_195.0 * cos(Math.toRadians(lat))),
            (heading + random.nextDouble(-maxDeg, maxDeg) + 360) % 360,
            meters
        )
    }

    private fun BusTracker.follow(sample: Sample) = locate("7", sample.lat, sample.lng, sample.heading, 0L)

    @Test
    fun `bus on the second pass through Las Cloacas is not placed on the first pass`() {
        val random = Random(1)
        val tracker = BusTracker(line10.route)
        val stop = line10.stop("p066-te")
        // Vuelve por la Ruta 5 (inequívoco), entra a Las Cloacas, da la vuelta y llega a p066-te
        for (sample in line10.drive(300, stop)) {
            val located = tracker.follow(sample.noisy(random)).single()
            assertTrue("at ${sample.meters} located $located", line10.error(located, sample.meters) < 60)
        }
        val last = line10.drive(stop - 1, stop).last()
        val meters = tracker.follow(last).single()
        assertTrue(line10.route.metersToStop(meters, stop)!! < 50)
    }

    @Test
    fun `bus leaving Las Cloacas is not placed back on the road where it came in`() {
        val random = Random(2)
        val tracker = BusTracker(line10.route)
        val stop = line10.stop("p003-te")
        // Llega por la Ruta 5, entra, pasa p003-te, da la vuelta y sale otra vez a la Ruta 5 hacia el oeste
        for (sample in line10.drive(17, 56)) {
            val located = tracker.follow(sample.noisy(random)).single()
            assertTrue("at ${sample.meters} located $located", line10.error(located, sample.meters) < 60)
            if (sample.meters > line10.cumulative[stop] + 60) {
                assertTrue("at ${sample.meters} located $located", line10.route.hasPassed(located, stop))
            }
        }
    }

    @Test
    fun `bus first seen where the route repeats can be on either pass until it leaves`() {
        val tracker = BusTracker(line10.route)
        val inside = line10.drive(326, 327).first() // hacia el norte dentro de Las Cloacas, en la vuelta
        val positions = tracker.follow(inside)
        assertEquals(2, positions.size)
        assertTrue(positions.any { line10.error(it, inside.meters) < 5 })
        // Sigue siendo ambiguo: no se adivinó una pasada
        assertEquals(2, tracker.follow(inside).size)
        // Al salir hacia el este queda en la vuelta
        val out = line10.drive(335, 336).first()
        assertTrue(line10.error(tracker.follow(out).single(), out.meters) < 5)
    }

    @Test
    fun `a bus that leaves service is forgotten`() {
        val tracker = BusTracker(line10.route)
        tracker.follow(line10.drive(303, 304).first()) // en la vuelta
        tracker.retainBuses(emptySet())
        assertEquals(2, tracker.follow(line10.drive(326, 327).first()).size)
    }

    @Test
    fun `an old sighting is not used`() {
        val tracker = BusTracker(line10.route)
        val before = line10.drive(303, 304).first()
        val inside = line10.drive(326, 327).first()
        tracker.locate("7", before.lat, before.lng, before.heading, 0L)
        assertEquals(2, tracker.locate("7", inside.lat, inside.lng, inside.heading, 6 * 60_000L).size)
    }

    @Test
    fun `a full lap is followed on every route`() {
        for (fixture in listOf("rutaLinea_10.json", "rutaLinea_11.json", "rutaLinea_22.json")) {
            val line = Line(fixture)
            val random = Random(3)
            val tracker = BusTracker(line.route)
            var worst = 0.0
            for (sample in line.drive(0, line.nodes.lastIndex)) {
                val positions = tracker.follow(sample.noisy(random))
                assertEquals("$fixture at ${sample.meters}: $positions", 1, positions.size)
                worst = maxOf(worst, line.error(positions.single(), sample.meters))
            }
            // La línea 11 tiene un tramo de 32 m dibujado ida y vuelta (nodos 380–383): ahí el error llega a 72 m
            assertTrue("$fixture worst=$worst", worst < 80)
        }
    }
}

private fun bearing(a: RouteNode, b: RouteNode): Double {
    val f1 = Math.toRadians(a.lat)
    val f2 = Math.toRadians(b.lat)
    val dl = Math.toRadians(b.lng - a.lng)
    val y = sin(dl) * cos(f2)
    val x = cos(f1) * sin(f2) - sin(f1) * cos(f2) * cos(dl)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}
