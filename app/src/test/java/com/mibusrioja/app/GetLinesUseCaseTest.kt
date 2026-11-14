package com.mibusrioja.app

import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.usecase.GetLinesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetLinesUseCaseTest {

    private val useCase = GetLinesUseCase(BusRepository(FakeMireBusApi(), FakeNominatimApi()))

    @Test
    fun `aplana los grupos correctamente`() = runTest {
        val groups = useCase().getOrThrow()
        assertEquals(
            listOf("TRONCALES", "COMPLEMENTARIAS", "RURALES", "SERVICIOS ESPECIALES", "CHILECITO"),
            groups.map { it.nombre }
        )
    }

    @Test
    fun `troncales contiene lineas 10 11 12`() = runTest {
        val groups = useCase().getOrThrow()
        val troncales = groups.first { it.nombre == "TRONCALES" }
        assertEquals(listOf("10", "11", "12"), troncales.lineas.map { it.codLinea })
    }

    @Test
    fun `chilecito contiene lineas 80 y 81`() = runTest {
        val groups = useCase().getOrThrow()
        val chilecito = groups.first { it.nombre == "CHILECITO" }
        val codigos = chilecito.lineas.map { it.codLinea }
        assertTrue("80" in codigos && "81" in codigos)
    }

    @Test
    fun `todas las lineas tienen grupo asignado`() = runTest {
        val groups = useCase().getOrThrow()
        assertTrue(groups.flatMap { it.lineas }.all { it.grupo.isNotBlank() })
    }
}
