package com.mibusrioja.app

import com.mibusrioja.app.data.api.MireBusApi
import com.mibusrioja.app.data.api.NetworkModule
import com.mibusrioja.app.data.api.NominatimApi
import com.mibusrioja.app.data.api.model.*
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

object TestFixtures {

    val moshi = NetworkModule.provideMoshi()

    fun readFixture(name: String): String =
        javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    inline fun <reified T> parse(name: String): T =
        moshi.adapter(T::class.java).fromJson(readFixture(name))!!

    inline fun <reified T> parseJson(json: String): T =
        moshi.adapter(T::class.java).fromJson(json)!!
}

/**
 * API falsa que sirve los fixtures capturados del servidor real.
 * Permite sobreescribir respuestas puntuales para tests específicos.
 */
open class FakeMireBusApi(
    private val routeProvider: (String) -> RutaLineaResponse = {
        TestFixtures.parse("rutaLinea_10.json")
    },
    private val posicionesProvider: (String) -> PosicionResponse = {
        TestFixtures.parse("posicionesBuses_12.json")
    },
    private val saldoProvider: (SaldoRequest) -> SaldoResponse = {
        TestFixtures.parse("resultadoSaldo_error1.json")
    }
) : MireBusApi {

    override suspend fun getConfiguracion(): ConfigResponse =
        TestFixtures.parse("getConfiguracion.json")

    override suspend fun getGruposLineas(): GruposLineasResponse =
        TestFixtures.parse("gruposLineas.json")

    override suspend fun getRutaLinea(codLinea: String): RutaLineaResponse =
        routeProvider(codLinea)

    override suspend fun getPosicionBuses(codLinea: String): PosicionResponse =
        posicionesProvider(codLinea)

    override suspend fun getHorariosEstimados(
        codLinea: String,
        codParada: String
    ): HorariosEstimadosResponse = TestFixtures.parse("horariosEstimados_empty.json")

    override suspend fun getPuntosVenta(lat: Double, lng: Double): PuntosVentaResponse =
        TestFixtures.parse("puntosVenta.json")

    override suspend fun checkCardCredit(body: SaldoRequest): SaldoResponse =
        saldoProvider(body)

    override suspend fun getCaptcha(timestamp: Long): ResponseBody =
        ByteArray(0).toResponseBody()
}

class FakeNominatimApi : NominatimApi {
    override suspend fun search(
        q: String,
        format: String,
        limit: Int,
        viewbox: String,
        bounded: Int,
        cc: String,
        lang: String
    ): List<NominatimPlaceDto> = emptyList()
}
