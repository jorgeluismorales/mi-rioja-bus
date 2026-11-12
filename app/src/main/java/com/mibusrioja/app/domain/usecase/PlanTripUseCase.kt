package com.mibusrioja.app.domain.usecase

import android.location.Location
import com.mibusrioja.app.domain.model.*
import javax.inject.Inject
import kotlin.math.abs

/**
 * Planificador de viajes simple que busca líneas que conecten
 * el origen con el destino, calculando paradas más cercanas.
 */
class PlanTripUseCase @Inject constructor(
    private val getLinesUseCase: GetLinesUseCase,
    private val getRouteUseCase: GetRouteUseCase
) {
    /**
     * Dado un origen (lat/lng GPS) y destino (lat/lng),
     * devuelve el plan de viaje con pasos y combinaciones.
     */
    suspend operator fun invoke(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Result<TripPlan> = runCatching {
        val linesResult = getLinesUseCase()
        val allLines = linesResult.getOrThrow().flatMap { flattenLines(it) }

        // Buscar la mejor ruta directa o con 1 transbordo
        val directRoute = findBestDirectRoute(allLines, originLat, originLng, destLat, destLng)

        if (directRoute != null) {
            directRoute
        } else {
            // Intentar con transbordo: buscar línea A hasta punto intermedio, luego línea B
            findRouteWithTransfer(allLines, originLat, originLng, destLat, destLng)
                ?: TripPlan(
                    pasos = listOf(
                        TripStep(
                            tipo = TripStepType.CAMINAR,
                            instruccion = "No se encontró una ruta disponible. Intentá acercarte al centro.",
                            distanciaMetros = 0
                        )
                    )
                )
        }
    }

    private fun flattenLines(group: LineGroup): List<BusLine> =
        group.lineas + group.subGrupos.flatMap { flattenLines(it) }

    private suspend fun findBestDirectRoute(
        lines: List<BusLine>,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): TripPlan? {
        var bestPlan: TripPlan? = null
        var bestScore = Double.MAX_VALUE

        for (line in lines) {
            val routeResult = getRouteUseCase(line.codLinea)
            val nodes = routeResult.getOrNull() ?: continue
            val stops = nodes.filter { it.esParada }
            if (stops.size < 2) continue

            // Parada más cercana al origen
            val boardStop = stops.minByOrNull { distanceTo(it.lat, it.lng, originLat, originLng) } ?: continue
            val boardDist = distanceTo(boardStop.lat, boardStop.lng, originLat, originLng)

            // Parada más cercana al destino (después de la parada de abordaje)
            val boardIdx = stops.indexOf(boardStop)
            val stopsAfterBoard = stops.drop(boardIdx + 1)
            if (stopsAfterBoard.isEmpty()) continue

            val alightStop = stopsAfterBoard.minByOrNull { distanceTo(it.lat, it.lng, destLat, destLng) } ?: continue
            val alightDist = distanceTo(alightStop.lat, alightStop.lng, destLat, destLng)

            val score = boardDist + alightDist
            if (score < bestScore) {
                bestScore = score
                bestPlan = TripPlan(
                    pasos = listOf(
                        TripStep(
                            tipo = TripStepType.CAMINAR,
                            instruccion = "Caminá ${boardDist.toInt()}m hasta la parada ${boardStop.nombreParada ?: "más cercana"}",
                            distanciaMetros = boardDist.toInt(),
                            paradaOrigen = boardStop
                        ),
                        TripStep(
                            tipo = TripStepType.TOMAR_COLECTIVO,
                            linea = line,
                            paradaOrigen = boardStop,
                            paradaDestino = alightStop,
                            instruccion = "Tomá el colectivo ${line.descripcion} y bajate en ${alightStop.nombreParada ?: "la parada indicada"}"
                        ),
                        TripStep(
                            tipo = TripStepType.CAMINAR,
                            instruccion = "Caminá ${alightDist.toInt()}m hasta tu destino",
                            distanciaMetros = alightDist.toInt()
                        ),
                        TripStep(
                            tipo = TripStepType.LLEGAR,
                            instruccion = "¡Llegaste a tu destino!"
                        )
                    )
                )
            }
        }
        // Solo considerar ruta directa si la parada de abordaje está a menos de 800m
        return if (bestScore < 800.0 + 800.0) bestPlan else null
    }

    private suspend fun findRouteWithTransfer(
        lines: List<BusLine>,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): TripPlan? {
        // Centro de La Rioja como punto de transbordo central
        val centerLat = -29.4134003
        val centerLng = -66.8560292

        val lineA = findBestDirectRoute(lines, originLat, originLng, centerLat, centerLng)
        val lineB = findBestDirectRoute(lines, centerLat, centerLng, destLat, destLng)

        if (lineA == null || lineB == null) return null

        val transferStop = lineA.pasos
            .filterIsInstance<TripStep>()
            .lastOrNull { it.paradaDestino != null }
            ?.paradaDestino ?: return null

        val allSteps = mutableListOf<TripStep>()
        allSteps.addAll(lineA.pasos.dropLast(1)) // quitar "llegaste"
        allSteps.add(
            TripStep(
                tipo = TripStepType.TRANSBORDO,
                paradaOrigen = transferStop,
                instruccion = "Transbordo en ${transferStop.nombreParada ?: "parada central"} — esperá el próximo colectivo"
            )
        )
        allSteps.addAll(lineB.pasos)

        return TripPlan(pasos = allSteps)
    }

    private fun distanceTo(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0].toDouble()
    }
}
