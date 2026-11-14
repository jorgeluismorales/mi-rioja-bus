package com.mibusrioja.app.data.api

import com.mibusrioja.app.data.api.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MireBusApi {

    /** Configuración general: tarifas, centro del mapa, tiles URL */
    @GET("rest/getConfiguracion")
    suspend fun getConfiguracion(): ConfigResponse

    /** Árbol de grupos y líneas de colectivos */
    @GET("rest/gruposLineas")
    suspend fun getGruposLineas(): GruposLineasResponse

    /**
     * Recorrido completo de una línea.
     * Devuelve lista de nodos con lat/lng, flag parada y descripción.
     */
    @GET("rest/rutaLinea/{codLinea}")
    suspend fun getRutaLinea(@Path("codLinea") codLinea: String): RutaLineaResponse

    /**
     * Posición en tiempo real de todos los colectivos de una línea.
     * Se debe llamar con polling cada 15s.
     */
    @GET("rest/posicionesBuses/{codLinea}")
    suspend fun getPosicionBuses(@Path("codLinea") codLinea: String): PosicionResponse

    /**
     * Tiempos estimados de llegada a una parada específica.
     * "Cuando viene"
     */
    @GET("rest/horariosEstimados/{codLinea}/{codParada}")
    suspend fun getHorariosEstimados(
        @Path("codLinea") codLinea: String,
        @Path("codParada") codParada: String
    ): HorariosEstimadosResponse

    /** Puntos de venta/recarga de tarjetas cercanos a una coordenada */
    @GET("rest/getPuntosVentaCercanos/{lat}/{lng}/1")
    suspend fun getPuntosVenta(
        @Path("lat") lat: Double,
        @Path("lng") lng: Double
    ): PuntosVentaResponse

    /** Consulta de saldo de tarjeta (sin login) */
    @POST("rest/tarjetaInternal/resultadoSaldo")
    suspend fun checkCardCredit(@Body body: SaldoRequest): SaldoResponse

    /** Imagen de captcha atada a la cookie de sesión */
    @GET("captcha.png")
    suspend fun getCaptcha(@Query("t") timestamp: Long): okhttp3.ResponseBody
}
