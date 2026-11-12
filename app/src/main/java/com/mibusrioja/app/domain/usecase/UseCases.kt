package com.mibusrioja.app.domain.usecase

import com.mibusrioja.app.data.api.model.GrupoDto
import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.model.*
import javax.inject.Inject

class GetLinesUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(): Result<List<LineGroup>> = repo.getGruposLineas().map { response ->
        response.grupos?.subGrupos?.map { mapGrupo(it) } ?: emptyList()
    }

    private fun mapGrupo(dto: GrupoDto): LineGroup = LineGroup(
        nombre = dto.codGrupo,
        lineas = dto.lineas?.map { BusLine(it.codLinea, it.descripcion, dto.codGrupo) } ?: emptyList(),
        subGrupos = dto.subGrupos?.map { mapGrupo(it) } ?: emptyList()
    )
}

class GetConfigUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(): Result<AppConfig> = repo.getConfiguracion().map { r ->
        val tarifas = r.menu
            .firstOrNull { it.tipo == "tarifas" }
            ?.contenidoJSON
            ?.filter { it.key.isNotBlank() && it.value.isNotBlank() }
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
                    codParada = nodo.codParada,
                    nombreParada = nodo.descripcionParada
                )
            }
        }
}

class GetBusPositionsUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(codLinea: String): Result<List<BusPosition>> =
        repo.getPosicionBuses(codLinea).map { r ->
            r.posiciones
                .filter { !it.fuera }
                .map { pos ->
                    BusPosition(
                        lat = pos.latitud,
                        lng = pos.longitud,
                        orientacion = pos.orientacion,
                        interno = pos.interno,
                        tieneRampa = pos.vehiculoRampa,
                        estaFuera = pos.fuera
                    )
                }
        }
}

class GetParadasCercanasUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(
        codLinea: String,
        codParada: String
    ): Result<List<TiempoEstimado>> =
        repo.getParadasCercanas(codLinea, codParada).map { r ->
            val horarios = r.horariosServicio
                ?: r.servicios?.flatMap { it.horariosServicio }
                ?: emptyList()
            horarios.take(3).map { h ->
                TiempoEstimado(
                    segundosFaltantes = h.tiempoFaltante,
                    interno = h.interno
                )
            }
        }
}

class GetPuntosVentaUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(lat: Double? = null, lng: Double? = null): Result<List<PuntoVenta>> =
        repo.getPuntosVenta(lat, lng).map { r ->
            r.puntosVenta.map { p ->
                PuntoVenta(
                    nombre = p.nombre,
                    direccion = p.direccion,
                    lat = p.latitud,
                    lng = p.longitud,
                    tipo = when (p.tipo.lowercase()) {
                        "atm" -> TipoPuntoVenta.ATM
                        "abono" -> TipoPuntoVenta.ABONO
                        else -> TipoPuntoVenta.COMUN
                    },
                    horario = p.horario
                )
            }
        }
}

class CheckBalanceUseCase @Inject constructor(private val repo: BusRepository) {
    suspend operator fun invoke(nroTarjeta: String, captcha: String? = null): Result<Saldo> =
        repo.checkCardCredit(nroTarjeta, captcha).map { r ->
            Saldo(
                nroTarjeta = r.nroTarjeta ?: nroTarjeta,
                saldo = r.saldo ?: 0.0,
                tipo = r.tipo ?: "Desconocido",
                movimientos = r.movimientos?.map { m ->
                    Movimiento(
                        fecha = m.fecha,
                        monto = m.monto,
                        tipo = m.tipo,
                        descripcion = m.descripcion,
                        linea = m.linea
                    )
                } ?: emptyList()
            )
        }
}
