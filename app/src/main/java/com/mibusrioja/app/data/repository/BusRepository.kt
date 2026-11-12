package com.mibusrioja.app.data.repository

import com.mibusrioja.app.data.api.MireBusApi
import com.mibusrioja.app.data.api.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val api: MireBusApi
) {
    /** Config general (tarifas, mapa, tiles) */
    suspend fun getConfiguracion(): Result<ConfigResponse> = runCatching {
        api.getConfiguracion()
    }

    /** Árbol de grupos/líneas */
    suspend fun getGruposLineas(): Result<GruposLineasResponse> = runCatching {
        api.getGruposLineas()
    }

    /** Recorrido completo con paradas (polyline) */
    suspend fun getRutaLinea(codLinea: String): Result<RutaLineaResponse> = runCatching {
        api.getRutaLinea(codLinea)
    }

    /** Posición en tiempo real de los buses */
    suspend fun getPosicionBuses(codLinea: String): Result<PosicionResponse> = runCatching {
        api.getPosicionBuses(codLinea)
    }

    /** Tiempos de llegada a una parada (cuando viene) */
    suspend fun getParadasCercanas(
        codLinea: String,
        codParada: String
    ): Result<ParadasCercanasResponse> = runCatching {
        api.getParadasCercanas(codLinea, codParada)
    }

    /** Puntos de venta */
    suspend fun getPuntosVenta(
        lat: Double? = null,
        lng: Double? = null
    ): Result<PuntosVentaResponse> = runCatching {
        api.getPuntosVenta(lat, lng)
    }

    /** Saldo de tarjeta (anónimo) */
    suspend fun checkCardCredit(
        nroTarjeta: String,
        captcha: String? = null
    ): Result<SaldoResponse> = runCatching {
        api.checkCardCredit(nroTarjeta, captcha)
    }
}
