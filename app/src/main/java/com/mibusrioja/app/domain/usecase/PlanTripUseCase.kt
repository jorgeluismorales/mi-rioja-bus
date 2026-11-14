package com.mibusrioja.app.domain.usecase

import com.mibusrioja.app.domain.geo.GeoUtils
import com.mibusrioja.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Planificador de viajes que busca la mejor combinación de líneas
 * (directa o con un transbordo) entre el origen y el destino.
 */
class PlanTripUseCase @Inject constructor(
    private val getLinesUseCase: GetLinesUseCase,
    private val getRouteUseCase: GetRouteUseCase
) {
    private data class StopRef(val nodeIndex: Int, val node: RouteNode)

    private data class LineRoute(val line: BusLine, val nodes: List<RouteNode>, val stops: List<StopRef>)

    suspend operator fun invoke(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Result<TripPlan> = runCatching {
        val allLines = getLinesUseCase().getOrThrow().flatMap { flattenLines(it) }

        // Descargar todos los recorridos en paralelo (el repositorio los cachea)
        val routes: List<LineRoute> = coroutineScope {
            allLines.map { line ->
                async {
                    val nodes = getRouteUseCase(line.codLinea).getOrNull() ?: return@async null
                    val stops = nodes.mapIndexedNotNull { i, n -> if (n.esParada) StopRef(i, n) else null }
                    if (stops.size < 2) null else LineRoute(line, nodes, stops)
                }
            }.mapNotNull { it.await() }
        }

        withContext(Dispatchers.Default) {
            val direct = findBestDirect(routes, originLat, originLng, destLat, destLng)
            val plan = direct ?: findBestTransfer(routes, originLat, originLng, destLat, destLng)
            plan ?: throw NoRouteException(
                "No encontramos un recorrido de colectivo entre tu ubicación y ese destino (buscamos paradas hasta 800 m)."
            )
        }
    }

    private fun flattenLines(group: LineGroup): List<BusLine> =
        group.lineas + group.subGrupos.flatMap { flattenLines(it) }

    private fun dist(a: RouteNode, lat: Double, lng: Double) =
        GeoUtils.distanceMeters(a.lat, a.lng, lat, lng)

    private fun recorridoEntre(nodes: List<RouteNode>, fromIndex: Int, toIndex: Int): List<RouteNode> =
        nodes.subList(minOf(fromIndex, toIndex), maxOf(fromIndex, toIndex) + 1)

    private fun recorridoMetros(recorrido: List<RouteNode>): Double {
        var total = 0.0
        for (i in 0 until recorrido.size - 1) {
            total += GeoUtils.distanceMeters(
                recorrido[i].lat, recorrido[i].lng,
                recorrido[i + 1].lat, recorrido[i + 1].lng
            )
        }
        return total
    }

    private fun duracionMin(plan: List<TripStep>, transbordos: Int): Int {
        var walk = 0.0
        var bus = 0.0
        for (step in plan) {
            when (step.tipo) {
                TripStepType.CAMINAR -> walk += step.distanciaMetros
                TripStepType.TRANSBORDO -> walk += step.distanciaMetros
                TripStepType.TOMAR_COLECTIVO -> bus += recorridoMetros(step.recorrido)
                TripStepType.LLEGAR -> {}
            }
        }
        return (walk / 80.0 + bus / 400.0 + transbordos * 5).roundToInt().coerceAtLeast(1)
    }

    private fun findBestDirect(
        routes: List<LineRoute>,
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double
    ): TripPlan? {
        var best: TripPlan? = null
        var bestScore = Double.MAX_VALUE

        for (route in routes) {
            val board = route.stops.filter { dist(it.node, originLat, originLng) <= 800.0 }
            if (board.isEmpty()) continue
            for (a in board) {
                val walkOrigen = dist(a.node, originLat, originLng)
                val alightCandidates = route.stops.filter { it.nodeIndex > a.nodeIndex && dist(it.node, destLat, destLng) <= 800.0 }
                for (b in alightCandidates) {
                    val walkDestino = dist(b.node, destLat, destLng)
                    val score = walkOrigen + walkDestino
                    if (score < bestScore) {
                        bestScore = score
                        val recorrido = recorridoEntre(route.nodes, a.nodeIndex, b.nodeIndex)
                        best = buildDirectPlan(
                            route.line, a.node, b.node, recorrido,
                            walkOrigen, walkDestino, originLat, originLng, destLat, destLng
                        )
                    }
                }
            }
        }
        return best
    }

    private fun buildDirectPlan(
        line: BusLine, board: RouteNode, alight: RouteNode, recorrido: List<RouteNode>,
        walkOrigen: Double, walkDestino: Double,
        originLat: Double, originLng: Double, destLat: Double, destLng: Double
    ): TripPlan {
        val pasos = listOf(
            TripStep(
                tipo = TripStepType.CAMINAR,
                paradaDestino = board,
                instruccion = "Caminá ${walkOrigen.roundToInt()} m hasta la parada ${board.nombreParada ?: "más cercana"}",
                distanciaMetros = walkOrigen.roundToInt()
            ),
            TripStep(
                tipo = TripStepType.TOMAR_COLECTIVO,
                linea = line,
                paradaOrigen = board,
                paradaDestino = alight,
                instruccion = "Tomá el colectivo ${line.descripcion} y bajate en ${alight.nombreParada ?: "la parada indicada"}",
                recorrido = recorrido
            ),
            TripStep(
                tipo = TripStepType.CAMINAR,
                paradaOrigen = alight,
                instruccion = "Caminá ${walkDestino.roundToInt()} m hasta tu destino",
                distanciaMetros = walkDestino.roundToInt()
            ),
            TripStep(
                tipo = TripStepType.LLEGAR,
                instruccion = "¡Llegaste a tu destino!"
            )
        )
        return TripPlan(
            pasos = pasos,
            origen = originLat to originLng,
            destino = destLat to destLng,
            duracionEstimadaMin = duracionMin(pasos, 0)
        )
    }

    private fun findBestTransfer(
        routes: List<LineRoute>,
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double
    ): TripPlan? {
        var best: TripPlan? = null
        var bestScore = Double.MAX_VALUE

        for (a in routes) {
            for (b in routes) {
                if (a === b) continue

                val boardOptions = a.stops.filter { dist(it.node, originLat, originLng) <= 800.0 }
                if (boardOptions.isEmpty()) continue
                val alightOptions = b.stops.filter { dist(it.node, destLat, destLng) <= 800.0 }
                if (alightOptions.isEmpty()) continue

                for (sa in boardOptions) {
                    val walkOrigen = dist(sa.node, originLat, originLng)
                    // x: parada de A posterior al abordaje donde se baja para transbordar
                    val xOptions = a.stops.filter { it.nodeIndex > sa.nodeIndex }
                    for (sb in alightOptions) {
                        val walkDestino = dist(sb.node, destLat, destLng)
                        // y: parada de B anterior a la bajada donde se sube el segundo colectivo
                        val yOptions = b.stops.filter { it.nodeIndex < sb.nodeIndex }
                        for (x in xOptions) {
                            for (y in yOptions) {
                                val dxy = GeoUtils.distanceMeters(x.node.lat, x.node.lng, y.node.lat, y.node.lng)
                                if (dxy > 300.0) continue
                                val score = walkOrigen + 1.5 * dxy + walkDestino
                                if (score < bestScore) {
                                    bestScore = score
                                    best = buildTransferPlan(
                                        a, b, sa.node, x.node, y.node, sb.node, dxy,
                                        walkOrigen, walkDestino,
                                        originLat, originLng, destLat, destLng
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return best
    }

    private fun buildTransferPlan(
        a: LineRoute, b: LineRoute,
        board: RouteNode, x: RouteNode, y: RouteNode, alight: RouteNode,
        distTransbordo: Double,
        walkOrigen: Double, walkDestino: Double,
        originLat: Double, originLng: Double, destLat: Double, destLng: Double
    ): TripPlan {
        val recorridoA = recorridoEntre(a.nodes, a.nodes.indexOf(board), a.nodes.indexOf(x))
        val recorridoB = recorridoEntre(b.nodes, b.nodes.indexOf(y), b.nodes.indexOf(alight))
        val pasos = listOf(
            TripStep(
                tipo = TripStepType.CAMINAR,
                paradaDestino = board,
                instruccion = "Caminá ${walkOrigen.roundToInt()} m hasta la parada ${board.nombreParada ?: "más cercana"}",
                distanciaMetros = walkOrigen.roundToInt()
            ),
            TripStep(
                tipo = TripStepType.TOMAR_COLECTIVO,
                linea = a.line,
                paradaOrigen = board,
                paradaDestino = x,
                instruccion = "Tomá el colectivo ${a.line.descripcion} y bajate en ${x.nombreParada ?: "la parada de transbordo"}",
                recorrido = recorridoA
            ),
            TripStep(
                tipo = TripStepType.TRANSBORDO,
                linea = b.line,
                paradaOrigen = x,
                paradaDestino = y,
                instruccion = "Caminá ${distTransbordo.roundToInt()} m hasta la parada de la línea ${b.line.descripcion}",
                distanciaMetros = distTransbordo.roundToInt()
            ),
            TripStep(
                tipo = TripStepType.TOMAR_COLECTIVO,
                linea = b.line,
                paradaOrigen = y,
                paradaDestino = alight,
                instruccion = "Tomá el colectivo ${b.line.descripcion} y bajate en ${alight.nombreParada ?: "la parada indicada"}",
                recorrido = recorridoB
            ),
            TripStep(
                tipo = TripStepType.CAMINAR,
                paradaOrigen = alight,
                instruccion = "Caminá ${walkDestino.roundToInt()} m hasta tu destino",
                distanciaMetros = walkDestino.roundToInt()
            ),
            TripStep(
                tipo = TripStepType.LLEGAR,
                instruccion = "¡Llegaste a tu destino!"
            )
        )
        return TripPlan(
            pasos = pasos,
            origen = originLat to originLng,
            destino = destLat to destLng,
            duracionEstimadaMin = duracionMin(pasos, 1)
        )
    }
}
