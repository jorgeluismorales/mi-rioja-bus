package com.mibusrioja.app.domain.usecase

import com.mibusrioja.app.data.api.model.GrupoDto
import com.mibusrioja.app.data.api.model.SaldoRequest
import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.model.*
import javax.inject.Inject

class GetLinesUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(): Result<List<LineGroup>> = repo.getGruposLineas().map { response ->
        response.grupos?.subGrupos?.flatMap { flattenNode(it) } ?: emptyList()
    }

    /**
     * Si todos los subGrupos de un nodo son hojas (tienen lineas, sin subGrupos)
     * cada hoja es su propio LineGroup; si el nodo tiene lineas propias se emite
     * tal cual; si la anidación es más profunda se fusiona todo en un solo grupo
     * con el nombre del nodo.
     */
    private fun flattenNode(node: GrupoDto): List<LineGroup> {
        val subs = node.subGrupos.orEmpty()
        val allLeaves = subs.isNotEmpty() && subs.all { it.subGrupos.isNullOrEmpty() && !it.lineas.isNullOrEmpty() }
        return when {
            allLeaves -> subs.map { leaf ->
                LineGroup(
                    nombre = leaf.codGrupo,
                    lineas = leaf.lineas.orEmpty().map { BusLine(it.codLinea, it.descripcion, leaf.codGrupo) }
                )
            }
            !node.lineas.isNullOrEmpty() -> listOf(
                LineGroup(
                    nombre = node.codGrupo,
                    lineas = node.lineas.orEmpty().map { BusLine(it.codLinea, it.descripcion, node.codGrupo) }
                )
            )
            else -> {
                val nombre = node.codGrupo.removePrefix("RIOJA BUS ").trim()
                listOf(
                    LineGroup(
                        nombre = nombre,
                        lineas = collectLines(node).map { BusLine(it.codLinea, it.descripcion, nombre) }
                    )
                )
            }
        }
    }

    private fun collectLines(node: GrupoDto): List<com.mibusrioja.app.data.api.model.LineaDto> =
        node.lineas.orEmpty() + node.subGrupos.orEmpty().flatMap { collectLines(it) }
}

class GetConfigUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(): Result<AppConfig> = repo.getConfiguracion().map { r ->
        val tarifas = r.menu
            .firstOrNull { it.tipo == "tarifas" }
            ?.contenidoJSON
            ?.filter { it.key.isNotBlank() && it.value.isNotBlank() && !it.value.matches(Regex("^\\$0+,00$")) }
            ?.map { TarifaItem(it.key.trimEnd(':').trim(), it.value) }
            ?: emptyList()
        AppConfig(
            latCenter = r.latitud,
            lngCenter = r.longitud,
            urlPrincipalMapas = r.urlPrincipalMapas,
            urlSecundariaMapas = r.urlSecundariaMapas,
            tarifas = tarifas
        )
    }
}

class GetRouteUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(codLinea: String): Result<List<RouteNode>> =
        repo.getRutaLinea(codLinea).map { r ->
            r.nodos.map { nodo ->
                RouteNode(
                    lat = nodo.latitud,
                    lng = nodo.longitud,
                    esParada = nodo.parada,
                    codParada = nodo.codigoParada?.takeIf { it.isNotBlank() },
                    nombreParada = nodo.descripcionParada
                )
            }
        }
}

class GetBusPositionsUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(codLinea: String): Result<List<BusPosition>> =
        repo.getPosicionBuses(codLinea).map { r ->
            r.posiciones
                .filter { !it.vehiculoNoVisibles }
                .map { pos ->
                    BusPosition(
                        lat = pos.latitud,
                        lng = pos.longitud,
                        orientacion = pos.orientacion,
                        interno = pos.interno,
                        tieneRampa = pos.vehiculoRampa,
                        proximaParada = pos.proximaParada
                    )
                }
        }
}

class GetArrivalTimesUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(
        codLinea: String,
        codParada: String
    ): Result<List<TiempoEstimado>> =
        repo.getHorariosEstimados(codLinea, codParada).map { r ->
            r.horariosServicio.orEmpty().take(3).map { h ->
                TiempoEstimado(
                    segundosFaltantes = h.tiempoFaltante,
                    interno = h.interno
                )
            }
        }
}

class GetPuntosVentaUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(lat: Double, lng: Double): Result<List<PuntoVenta>> =
        repo.getPuntosVenta(lat, lng).map { r ->
            r.puntosVenta.map { p ->
                val tipo = when (p.tipo) {
                    4 -> TipoPuntoVenta.ATM
                    2, 3 -> TipoPuntoVenta.ABONO
                    else -> TipoPuntoVenta.COMUN
                }
                PuntoVenta(
                    nombre = p.nombre,
                    direccion = buildString {
                        append(p.domicilio.orEmpty().trim())
                        p.detalleDomicilio?.takeIf { it.isNotBlank() }?.let { append(", $it") }
                    },
                    lat = p.latitud,
                    lng = p.longitud,
                    tipo = tipo,
                    descripcionTipo = when (p.tipo) {
                        1 -> "Venta de tarjetas comunes"
                        2 -> "Venta de tarjetas comunes y abonos varios"
                        3 -> "Abonos especiales"
                        4 -> "ATM"
                        5 -> "Punto de venta general"
                        6 -> "Recarga de tarjetas Metropolitana e Independencia"
                        else -> "Punto de venta"
                    }
                )
            }
        }
}

class CheckBalanceUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(nroTarjeta: String, captcha: String): Result<Saldo> =
        repo.checkCardCredit(
            SaldoRequest(verificacionCaptcha = captcha, nroExternoTarjeta = nroTarjeta)
        ).mapCatching { r ->
            if (r.error != 0) {
                throw SaldoException(r.error, mensajeParaError(r.error, r.mensaje))
            }
            val principal = r.saldos?.firstOrNull()
            Saldo(
                nroTarjeta = r.numeroTarjeta ?: nroTarjeta,
                saldo = parseSaldoValue(principal?.value),
                tipo = r.tipoTarjeta ?: "",
                estado = r.estadoTarjeta,
                fechaSaldo = r.fechaSaldo,
                etiqueta = principal?.label?.takeIf { it.isNotBlank() }
            )
        }

    private fun mensajeParaError(code: Int, mensaje: String?): String = when (code) {
        1 -> "El código de seguridad no es correcto. Probá de nuevo."
        2 -> "El número de tarjeta ingresado no es válido"
        3 -> "No se pudo realizar la operación"
        4 -> "No se pudo consultar el servicio"
        5 -> "Esta tarjeta no permite el cálculo de saldo en línea"
        else -> mensaje ?: "Error de conexión"
    }

    /**
     * Convierte el value del saldo a Double. El servicio real devuelve strings como
     * "2300.00 pasajes" o "$1.234,50": se extrae el primer número y se normalizan separadores.
     */
    internal fun parseSaldoValue(value: Any?): Double = when (value) {
        is Number -> value.toDouble()
        is String -> {
            val token = NUMBER_REGEX.find(value)?.value ?: return 0.0
            val normalized = when {
                token.contains('.') && token.contains(',') ->
                    if (token.lastIndexOf(',') > token.lastIndexOf('.')) token.replace(".", "").replace(',', '.')
                    else token.replace(",", "")
                token.contains(',') -> token.replace(',', '.')
                THOUSANDS_DOT_REGEX.matches(token) -> token.replace(".", "")
                else -> token
            }
            normalized.toDoubleOrNull() ?: 0.0
        }
        else -> 0.0
    }

    private companion object {
        val NUMBER_REGEX = Regex("""-?\d[\d.,]*""")
        val THOUSANDS_DOT_REGEX = Regex("""-?\d{1,3}(\.\d{3})+""")
    }
}

class SearchPlacesUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(query: String): Result<List<PlaceSuggestion>> =
        repo.searchPlaces(query).map { places ->
            places.mapNotNull { p ->
                val lat = p.lat.toDoubleOrNull() ?: return@mapNotNull null
                val lng = p.lon.toDoubleOrNull() ?: return@mapNotNull null
                PlaceSuggestion(
                    nombre = p.name?.takeIf { it.isNotBlank() }
                        ?: p.displayName.substringBefore(',').trim(),
                    detalle = p.displayName,
                    lat = lat,
                    lng = lng
                )
            }
        }
}
