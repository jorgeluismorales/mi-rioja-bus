package com.mibusrioja.app.data.api

import com.mibusrioja.app.data.api.model.*
import retrofit2.http.GET
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
    @GET("rest/posicion/{codLinea}")
    suspend fun getPosicionBuses(@Path("codLinea") codLinea: String): PosicionResponse

    /**
     * Tiempos estimados de llegada a una parada específica.
     * "Cuando viene"
     */
    @GET("rest/paradasCercanas/{codLinea}/{codParada}")
    suspend fun getParadasCercanas(
        @Path("codLinea") codLinea: String,
        @Path("codParada") codParada: String
    ): ParadasCercanasResponse

    /** Puntos de venta/recarga de tarjetas */
    @GET("rest/getPuntosVentaCercanos")
    suspend fun getPuntosVenta(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): PuntosVentaResponse

    /** Consulta de saldo de tarjeta (sin login) */
    @GET("rest/checkCardCredit")
    suspend fun checkCardCredit(
        @Query("nroTarjeta") nroTarjeta: String,
        @Query("captcha") captcha: String? = null
    ): SaldoResponse

    /** Captcha image — se muestra como WebView o imagen */
    @GET("captcha.png")
    suspend fun getCaptcha(): okhttp3.ResponseBody
}
