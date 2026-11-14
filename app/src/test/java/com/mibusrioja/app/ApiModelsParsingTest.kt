package com.mibusrioja.app

import com.mibusrioja.app.data.api.model.ConfigResponse
import com.mibusrioja.app.data.api.model.GruposLineasResponse
import com.mibusrioja.app.data.api.model.HorariosEstimadosResponse
import com.mibusrioja.app.data.api.model.PosicionResponse
import com.mibusrioja.app.data.api.model.PuntosVentaResponse
import com.mibusrioja.app.data.api.model.RutaLineaResponse
import com.mibusrioja.app.data.api.model.SaldoResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelsParsingTest {

    @Test
    fun `config fixture parses con tarifas`() {
        val config = TestFixtures.parse<ConfigResponse>("getConfiguracion.json")
        val tarifas = config.menu.first { it.tipo == "tarifas" }.contenidoJSON!!
        assertTrue(tarifas.size >= 19)
        assertEquals("Usuario Común:", tarifas.first().key)
        assertTrue(config.urlPrincipalMapas.isNotBlank())
        assertTrue(config.urlSecundariaMapas.isNotBlank())
    }

    @Test
    fun `grupos fixture parsea el arbol raiz`() {
        val grupos = TestFixtures.parse<GruposLineasResponse>("gruposLineas.json")
        assertEquals("Grupos de Lineas", grupos.grupos!!.codGrupo)
        assertEquals(2, grupos.grupos!!.subGrupos!!.size)
    }

    @Test
    fun `ruta fixture parsea nodos y paradas`() {
        val ruta = TestFixtures.parse<RutaLineaResponse>("rutaLinea_10.json")
        assertEquals(356, ruta.nodos.size)
        assertEquals(67, ruta.nodos.count { it.parada })
        assertEquals("p001-te", ruta.nodos.first().codigoParada)
    }

    @Test
    fun `posiciones fixture parsea internos`() {
        val pos = TestFixtures.parse<PosicionResponse>("posicionesBuses_12.json")
        assertTrue(pos.posiciones.isNotEmpty())
        assertTrue(pos.posiciones.all { it.interno.isNotBlank() })
    }

    @Test
    fun `puntos venta fixture parsea 59 puntos con tipos variados`() {
        val pv = TestFixtures.parse<PuntosVentaResponse>("puntosVenta.json")
        assertEquals(59, pv.puntosVenta.size)
        val tipos = pv.puntosVenta.map { it.tipo }.toSet()
        assertTrue(1 in tipos && 4 in tipos && null in tipos)
    }

    @Test
    fun `horarios estimados vacios parsea`() {
        val h = TestFixtures.parse<HorariosEstimadosResponse>("horariosEstimados_empty.json")
        assertEquals(0, h.error)
        assertEquals(emptyList<Any>(), h.horariosServicio)
    }

    @Test
    fun `resultado saldo error 1 parsea`() {
        val s = TestFixtures.parse<SaldoResponse>("resultadoSaldo_error1.json")
        assertEquals(1, s.error)
    }
}
