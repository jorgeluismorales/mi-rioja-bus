package com.mibusrioja.app

import com.mibusrioja.app.data.local.NoApiCache
import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.model.TipoPuntoVenta
import com.mibusrioja.app.domain.usecase.GetPuntosVentaUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPuntosVentaUseCaseTest {

    private val useCase = GetPuntosVentaUseCase(BusRepository(FakeMireBusApi(), FakeNominatimApi(), NoApiCache))

    @Test
    fun `mapea tipos numericos al enum`() = runTest {
        val puntos = useCase(-29.41, -66.85).getOrThrow()
        assertTrue(puntos.any { it.tipo == TipoPuntoVenta.ATM })
        assertTrue(puntos.any { it.tipo == TipoPuntoVenta.COMUN })
        // tipo null se mapea a COMUN
        assertEquals(59, puntos.size)
    }

    @Test
    fun `descripcionTipo venta comun para tipo 1`() = runTest {
        val puntos = useCase(-29.41, -66.85).getOrThrow()
        val comunes = puntos.filter { it.tipo == TipoPuntoVenta.COMUN }
        assertTrue(comunes.any { it.descripcionTipo == "Venta de tarjetas comunes" })
    }

    @Test
    fun `descripcionTipo ATM para tipo 4`() = runTest {
        val puntos = useCase(-29.41, -66.85).getOrThrow()
        val atm = puntos.first { it.tipo == TipoPuntoVenta.ATM }
        assertEquals("ATM", atm.descripcionTipo)
    }

    @Test
    fun `direccion viene recortada`() = runTest {
        val puntos = useCase(-29.41, -66.85).getOrThrow()
        assertTrue(puntos.all { it.direccion == it.direccion.trim() })
    }
}
