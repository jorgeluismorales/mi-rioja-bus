package com.mibusrioja.app

import com.mibusrioja.app.data.local.NoApiCache
import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.usecase.GetRouteUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetRouteUseCaseTest {

    private val useCase = GetRouteUseCase(BusRepository(FakeMireBusApi(), FakeNominatimApi(), NoApiCache))

    @Test
    fun `mapea 67 paradas con codigoParada`() = runTest {
        val nodes = useCase("10").getOrThrow()
        assertEquals(356, nodes.size)
        assertEquals(67, nodes.count { it.esParada })
    }

    @Test
    fun `primera parada tiene codigo y nombre`() = runTest {
        val nodes = useCase("10").getOrThrow()
        val first = nodes.first { it.esParada }
        assertEquals("p001-te", first.codParada)
        assertNotNull(first.nombreParada)
        assertTrue(first.nombreParada!!.isNotBlank())
    }

    @Test
    fun `nodos que no son parada tienen codParada null`() = runTest {
        val nodes = useCase("10").getOrThrow()
        assertTrue(nodes.filter { !it.esParada }.all { it.codParada == null })
    }
}
