package com.mibusrioja.app

import com.mibusrioja.app.data.api.model.PosicionDto
import com.mibusrioja.app.data.api.model.PosicionResponse
import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.usecase.GetBusPositionsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBusPositionsUseCaseTest {

    @Test
    fun `devuelve los buses visibles del fixture`() = runTest {
        val useCase = GetBusPositionsUseCase(BusRepository(FakeMireBusApi(), FakeNominatimApi()))
        val positions = useCase("12").getOrThrow()
        assertEquals(2, positions.size)
        assertTrue(positions.all { it.interno.isNotBlank() })
    }

    @Test
    fun `filtra vehiculos marcados como no visibles`() = runTest {
        val api = FakeMireBusApi(
            posicionesProvider = {
                PosicionResponse(
                    error = 0,
                    posiciones = TestFixtures.parse<PosicionResponse>("posicionesBuses_12.json").posiciones +
                        TestFixtures.parseJson<PosicionDto>(
                            """{"interno":"99","latitud":-29.4,"longitud":-66.8,"orientacion":0.0,"proximaParada":null,"vehiculoRampa":false,"vehiculoNoVisibles":true}"""
                        )
                )
            }
        )
        val useCase = GetBusPositionsUseCase(BusRepository(api, FakeNominatimApi()))
        val positions = useCase("12").getOrThrow()
        assertTrue(positions.none { it.interno == "99" })
        assertEquals(2, positions.size)
    }
}
