package com.mibusrioja.app.data.repository

import com.mibusrioja.app.data.api.MireBusApi
import com.mibusrioja.app.data.api.NominatimApi
import com.mibusrioja.app.data.api.model.*
import com.mibusrioja.app.data.local.ApiCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val api: MireBusApi,
    private val nominatimApi: NominatimApi,
    private val cache: ApiCache
) {
    /** Memoria de sesión; detrás está la caché persistente diaria ([ApiCache]). */
    private val memory = ConcurrentHashMap<String, Any>()

    /**
     * Datos estáticos: se piden al servidor sólo la primera vez en el día.
     * Sin red, se usan los datos guardados aunque sean de un día anterior.
     */
    private suspend inline fun <reified T : Any> daily(
        key: String,
        forceRefresh: Boolean,
        crossinline fetch: suspend () -> T
    ): Result<T> = runCatching {
        if (!forceRefresh) (memory[key] as? T)?.let { return@runCatching it }
        val stored = if (forceRefresh) null else withContext(Dispatchers.IO) { cache.read(key, T::class.java) }
        if (stored != null && stored.fresh) return@runCatching stored.data.also { memory[key] = it }
        val fresh = runCatching { fetch() }.getOrElse { e -> stored?.data ?: throw e }
        if (fresh !== stored?.data) withContext(Dispatchers.IO) { cache.write(key, T::class.java, fresh) }
        fresh.also { memory[key] = it }
    }

    /** Config general (tarifas, mapa, tiles). Una consulta por día. */
    suspend fun getConfiguracion(forceRefresh: Boolean = false): Result<ConfigResponse> =
        daily("config", forceRefresh) { api.getConfiguracion() }

    /** Árbol de grupos/líneas. Una consulta por día. */
    suspend fun getGruposLineas(forceRefresh: Boolean = false): Result<GruposLineasResponse> =
        daily("grupos_lineas", forceRefresh) { api.getGruposLineas() }

    /** Recorrido completo con paradas (polyline). Una consulta por línea y por día. */
    suspend fun getRutaLinea(
        codLinea: String,
        forceRefresh: Boolean = false
    ): Result<RutaLineaResponse> =
        daily("ruta_$codLinea", forceRefresh) { api.getRutaLinea(codLinea) }

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
