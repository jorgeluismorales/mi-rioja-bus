package com.mibusrioja.app

import com.mibusrioja.app.domain.model.TarifaItem
import com.mibusrioja.app.domain.usecase.TarifaKind
import com.mibusrioja.app.domain.usecase.buildTarifaDisplays
import com.mibusrioja.app.domain.usecase.formatArgPrice
import com.mibusrioja.app.domain.usecase.parseArgPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TarifaPresenterTest {

    @Test
    fun `parseArgPrice parses Argentine formats`() {
        assertEquals(1000.0, parseArgPrice("$1.000,00")!!, 0.001)
        assertEquals(849.0, parseArgPrice("$849,00")!!, 0.001)
        assertNull(parseArgPrice("PASE LIBRE"))
    }

    @Test
    fun `formatArgPrice formats Argentine currency`() {
        assertEquals("$600,00", formatArgPrice(600.0))
        assertEquals("$1.000,00", formatArgPrice(1000.0))
    }

    private val items = listOf(
        TarifaItem("Usuario Común:", "$1.000,00"),
        TarifaItem("JUB-PEN:", "40% Dto."),
        TarifaItem("Discapacidad:", "PASE LIBRE"),
        TarifaItem("Rurales:", "$0000,00"),
        TarifaItem("Rurales:", "$500,00"),
        TarifaItem("Sin precio:", "Consultar")
    )

    @Test
    fun `descuento derives price from base`() {
        val jub = buildTarifaDisplays(items).first { it.categoria == "JUB-PEN" }
        assertEquals(TarifaKind.DESCUENTO, jub.kind)
        assertEquals("40% Dto.", jub.etiqueta)
        assertEquals("$600,00", jub.precioFinal)
        assertEquals("$1.000,00", jub.precioBase)
    }

    @Test
    fun `pase libre maps to gratis`() {
        val disc = buildTarifaDisplays(items).first { it.categoria == "Discapacidad" }
        assertEquals(TarifaKind.GRATIS, disc.kind)
        assertEquals("$0,00", disc.precioFinal)
        assertEquals("Pase Libre", disc.etiqueta)
    }

    @Test
    fun `zero estandar dropped and duplicates preserved`() {
        val rurales = buildTarifaDisplays(items).filter { it.categoria == "Rurales" }
        assertEquals(1, rurales.size)
        assertEquals("$500,00", rurales[0].precioFinal)
    }

    @Test
    fun `unparseable keeps raw and base fallback`() {
        val consultar = buildTarifaDisplays(items).first { it.categoria == "Sin precio" }
        assertEquals("Consultar", consultar.precioFinal)
        assertEquals(TarifaKind.ESTANDAR, consultar.kind)
    }

    @Test
    fun `no base price keeps raw discount text`() {
        val onlyDiscount = buildTarifaDisplays(listOf(TarifaItem("Estudiante:", "50% Dto.")))
        assertEquals("50% Dto.", onlyDiscount[0].precioFinal)
        assertNull(onlyDiscount[0].precioBase)
    }
}
