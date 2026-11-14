package com.mibusrioja.app.data.repository

import com.mibusrioja.app.data.api.MireBusApi
import com.mibusrioja.app.data.api.NominatimApi
import com.mibusrioja.app.data.api.model.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val api: MireBusApi,
    private val nominatimApi: NominatimApi
) {
    @Volatile private var configCache: ConfigResponse? = null
    @Volatile private var gruposLineasCache: GruposLineasResponse? = null
    private val rutaLineaCache = ConcurrentHashMap<String, RutaLineaResponse>()

    /** Config general (tarifas, mapa, tiles). Cacheada durante la sesión. */
    suspend fun getConfiguracion(forceRefresh: Boolean = false): Result<ConfigResponse> = runCatching {
        if (!forceRefresh) configCache?.let { return@runCatching it }
        api.getConfiguracion().also { configCache = it }
    }

    /** Árbol de grupos/líneas. Cacheado durante la sesión. */
    suspend fun getGruposLineas(forceRefresh: Boolean = false): Result<GruposLineasResponse> = runCatching {
        if (!forceRefresh) gruposLineasCache?.let { return@runCatching it }
        api.getGruposLineas().also { gruposLineasCache = it }
    }

    /** Recorrido completo con paradas (polyline). Cacheado por línea. */
    suspend fun getRutaLinea(
        codLinea: String,
        forceRefresh: Boolean = false
    ): Result<RutaLineaResponse> = runCatching {
        if (!forceRefresh) rutaLineaCache[codLinea]?.let { return@runCatching it }
        api.getRutaLinea(codLinea).also { rutaLineaCache[codLinea] = it }
    }

    /** Posición en tiempo real de los buses */
    suspend fun getPosicionBuses(codLinea: String): Result<PosicionResponse> = runCatching {
        api.getPosicionBuses(codLinea)
    }

    /** Tiempos de llegada a una parada (cuando viene) */
    suspend fun getHorariosEstimados(
        codLinea: String,
        codParada: String
    ): Result<HorariosEstimadosResponse> = runCatching {
        api.getHorariosEstimados(codLinea, codParada)
    }

    /** Puntos de venta cercanos a una coordenada */
    suspend fun getPuntosVenta(lat: Double, lng: Double): Result<PuntosVentaResponse> = runCatching {
        api.getPuntosVenta(lat, lng)
    }

    /** Saldo de tarjeta (anónimo, con captcha de sesión) */
    suspend fun checkCardCredit(request: SaldoRequest): Result<SaldoResponse> = runCatching {
        api.checkCardCredit(request)
    }

    /** Imagen de captcha asociada a la cookie de sesión */
    suspend fun getCaptcha(): Result<ByteArray> = runCatching {
        api.getCaptcha(System.currentTimeMillis()).bytes()
    }

    /** Búsqueda de lugares en La Rioja vía Nominatim */
    suspend fun searchPlaces(query: String): Result<List<NominatimPlaceDto>> = runCatching {
        nominatimApi.search(q = query)
    }
}
