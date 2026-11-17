package com.mibusrioja.app

import com.mibusrioja.app.data.local.NoApiCache
import com.mibusrioja.app.data.api.model.SaldoRequest
import com.mibusrioja.app.data.api.model.SaldoResponse
import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.model.SaldoException
import com.mibusrioja.app.domain.usecase.CheckBalanceUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckBalanceUseCaseTest {

    private fun useCase(api: FakeMireBusApi) =
        CheckBalanceUseCase(BusRepository(api, FakeNominatimApi(), NoApiCache))

    @Test
    fun `error 1 devuelve SaldoException con code 1`() = runTest {
        val result = useCase(FakeMireBusApi())("1234", "abcd")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is SaldoException)
        assertEquals(1, (ex as SaldoException).code)
    }

    @Test
    fun `saldo numerico parsea a double`() = runTest {
        val api = FakeMireBusApi(
            saldoProvider = { _: SaldoRequest ->
                TestFixtures.parseJson<SaldoResponse>(
                    """{"error":0,"numeroTarjeta":"5679782","tipoTarjeta":"Común","estadoTarjeta":"Activa","saldos":[{"value":1234.5}],"fechaSaldo":"2026-11-12"}"""
                )
            }
        )
        val saldo = useCase(api)("5679782", "abcd").getOrThrow()
        assertEquals(1234.5, saldo.saldo, 0.001)
        assertEquals("5679782", saldo.nroTarjeta)
        assertEquals("Activa", saldo.estado)
        assertEquals("2026-11-12", saldo.fechaSaldo)
    }

    @Test
    fun `respuesta real del servicio (value "2300_00 pasajes") parsea 2300`() = runTest {
        val api = FakeMireBusApi(
            saldoProvider = { _: SaldoRequest -> TestFixtures.parse<SaldoResponse>("resultadoSaldo_ok.json") }
        )
        val saldo = useCase(api)("3276226", "5wxg7").getOrThrow()
        assertEquals(2300.0, saldo.saldo, 0.001)
        assertEquals("3276226", saldo.nroTarjeta)
        assertEquals("COMUN", saldo.tipo)
        assertEquals("ACTIVA", saldo.estado)
        assertEquals("09/11/2026 22:19", saldo.fechaSaldo)
        assertEquals("Saldo Principal (Dinero)", saldo.etiqueta)
    }

    @Test
    fun `parseSaldoValue soporta distintos formatos`() {
        val uc = CheckBalanceUseCase(BusRepository(FakeMireBusApi(), FakeNominatimApi(), NoApiCache))
        assertEquals(2300.0, uc.parseSaldoValue("2300.00 pasajes"), 0.001)
        assertEquals(-150.5, uc.parseSaldoValue("-150.50"), 0.001)
        assertEquals(1234.5, uc.parseSaldoValue("$1.234,50"), 0.001)
        assertEquals(1234.5, uc.parseSaldoValue("1,234.50"), 0.001)
        assertEquals(2300.0, uc.parseSaldoValue("2.300"), 0.001)
        assertEquals(99.9, uc.parseSaldoValue("99,9"), 0.001)
        assertEquals(0.0, uc.parseSaldoValue("sin datos"), 0.001)
        assertEquals(0.0, uc.parseSaldoValue(null), 0.001)
    }

    @Test
    fun `saldo string con formato argentino parsea`() = runTest {
        val api = FakeMireBusApi(
            saldoProvider = { _: SaldoRequest ->
                TestFixtures.parseJson<SaldoResponse>(
                    """{"error":0,"numeroTarjeta":"5679782","saldos":[{"value":"$1.234,50"}]}"""
                )
            }
        )
        val saldo = useCase(api)("5679782", "abcd").getOrThrow()
        assertEquals(1234.5, saldo.saldo, 0.001)
    }
}
