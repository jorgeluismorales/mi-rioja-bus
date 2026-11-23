package com.mibusrioja.app

import com.mibusrioja.app.data.local.NoApiCache
import com.mibusrioja.app.data.api.model.RutaLineaResponse
import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.model.NoRouteException
import com.mibusrioja.app.domain.model.TripPlan
import com.mibusrioja.app.domain.model.TripStepType
import com.mibusrioja.app.domain.usecase.GetLinesUseCase
import com.mibusrioja.app.domain.usecase.GetRouteUseCase
import com.mibusrioja.app.domain.usecase.PlanTripUseCase
import com.mibusrioja.app.ui.planner.RIOJA_DESTINATIONS
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanTripUseCaseTest {

    // Solo la línea 10 tiene recorrido; el resto devuelve error/nodos vacíos
    private val api = FakeMireBusApi(
        routeProvider = { cod ->
            if (cod == "10") TestFixtures.parse("rutaLinea_10.json")
            else RutaLineaResponse(error = 1, nodos = emptyList())
        }
    )
    private val repo = BusRepository(api, FakeNominatimApi(), NoApiCache)
    private val planTrip = PlanTripUseCase(
        GetLinesUseCase(repo),
        GetRouteUseCase(repo)
    )

    @Test
    fun `encuentra ruta directa por linea 10`() = runTest {
        val ruta = TestFixtures.parse<RutaLineaResponse>("rutaLinea_10.json")
        val paradas = ruta.nodos.filter { it.parada }
        val board = paradas.first()
        val alight = paradas[29]

        // origen a ~50m de la primera parada, destino a ~50m de la parada 30
        val originLat = board.latitud + 0.0004
        val originLng = board.longitud
        val destLat = alight.latitud + 0.0004
        val destLng = alight.longitud

        val plan = planTrip(originLat, originLng, destLat, destLng).getOrThrow()

        val busStep = plan.pasos.first { it.tipo == TripStepType.TOMAR_COLECTIVO }
        assertEquals("10", busStep.linea!!.codLinea)
        assertEquals("p001-te", busStep.paradaOrigen!!.codParada)
        assertTrue(busStep.recorrido.isNotEmpty())
        assertTrue(plan.duracionEstimadaMin > 0)
    }

    // Líneas 11 (pasa por el origen y cerca de la UNLaR) y 22 (para dentro del barrio de la UNLaR)
    private val planTripNorteSur = BusRepository(
        FakeMireBusApi(
            routeProvider = { cod ->
                when (cod) {
                    "11" -> TestFixtures.parse("rutaLinea_11.json")
                    "22" -> TestFixtures.parse("rutaLinea_22.json")
                    else -> RutaLineaResponse(error = 1, nodos = emptyList())
                }
            }
        ),
        FakeNominatimApi(),
        NoApiCache
    ).let { PlanTripUseCase(GetLinesUseCase(it), GetRouteUseCase(it)) }

    private fun assertSoloLinea11(plan: TripPlan) {
        val busSteps = plan.pasos.filter { it.tipo == TripStepType.TOMAR_COLECTIVO }
        assertEquals(listOf("11"), busSteps.map { it.linea!!.codLinea })
        assertTrue(plan.pasos.none { it.tipo == TripStepType.TRANSBORDO })
    }

    @Test
    fun `de barrio norte a UNLaR recomienda un solo colectivo`() = runTest {
        val unlar = RIOJA_DESTINATIONS.first { it.title == "UNLaR" }
        assertSoloLinea11(planTripNorteSur(-29.3888087, -66.8231107, unlar.lat, unlar.lng).getOrThrow())
    }

    @Test
    fun `prefiere un directo caminando mas de 800 m antes que un transbordo`() = runTest {
        // La parada más cercana de la línea 11 queda a ~870 m; la línea 22 para a 70 m
        assertSoloLinea11(planTripNorteSur(-29.3888087, -66.8231107, -29.434720, -66.871530).getOrThrow())
    }

    @Test
    fun `origen lejano devuelve NoRouteException`() = runTest {
        val result = planTrip(-34.6, -58.4, -29.434720, -66.871530)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoRouteException)
    }
}
