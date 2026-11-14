package com.mibusrioja.app

import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.usecase.GetConfigUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GetConfigUseCaseTest {

    private val useCase = GetConfigUseCase(BusRepository(FakeMireBusApi(), FakeNominatimApi()))

    @Test
    fun `filtra tarifas con precio cero bogus`() = runTest {
        val config = useCase().getOrThrow()
        assertFalse(config.tarifas.any { it.precio.matches(Regex("^\\$0+,00$")) })
        assertFalse(config.tarifas.any { it.precio.contains("0000") })
    }

    @Test
    fun `primera tarifa es usuario comun`() = runTest {
        val config = useCase().getOrThrow()
        assertEquals("Usuario Común", config.tarifas.first().categoria)
        assertEquals("$1.000,00", config.tarifas.first().precio)
    }

    @Test
    fun `urls del fixture se propagan`() = runTest {
        val config = useCase().getOrThrow()
        assertEquals("https://mapmoblrj.red-bus.com.ar/tiles/{z}/{x}/{y}.png", config.urlPrincipalMapas)
        assertEquals("https://b.tile.openstreetmap.org/{z}/{x}/{y}.png?access_token=00", config.urlSecundariaMapas)
    }
}
