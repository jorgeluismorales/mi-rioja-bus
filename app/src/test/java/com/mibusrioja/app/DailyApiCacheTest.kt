package com.mibusrioja.app

import com.mibusrioja.app.data.api.model.ConfigResponse
import com.mibusrioja.app.data.local.DailyApiCache
import com.mibusrioja.app.data.repository.BusRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.LocalDate

class DailyApiCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class CountingApi(var fail: Boolean = false) : FakeMireBusApi() {
        var configCalls = 0
        override suspend fun getConfiguracion(): ConfigResponse {
            configCalls++
            if (fail) throw IOException("sin red")
            return super.getConfiguracion()
        }
    }

    private fun repo(api: CountingApi, day: () -> LocalDate) =
        BusRepository(api, FakeNominatimApi(), DailyApiCache(tmp.root, TestFixtures.moshi, day))

    @Test
    fun `same day hits network once even across repository instances`() = runTest {
        val api = CountingApi()
        val day = { LocalDate.of(2026, 11, 17) }
        repo(api, day).getConfiguracion().getOrThrow()
        repo(api, day).getConfiguracion().getOrThrow() // nueva "sesión", mismo día
        assertEquals(1, api.configCalls)
    }

    @Test
    fun `next day refetches`() = runTest {
        val api = CountingApi()
        repo(api) { LocalDate.of(2026, 11, 17) }.getConfiguracion().getOrThrow()
        repo(api) { LocalDate.of(2026, 11, 18) }.getConfiguracion().getOrThrow()
        assertEquals(2, api.configCalls)
    }

    @Test
    fun `stale data used when network fails`() = runTest {
        val api = CountingApi()
        val first = repo(api) { LocalDate.of(2026, 11, 17) }.getConfiguracion().getOrThrow()
        api.fail = true
        val result = repo(api) { LocalDate.of(2026, 11, 18) }.getConfiguracion()
        assertTrue(result.isSuccess)
        assertEquals(first, result.getOrThrow())
        assertEquals(2, api.configCalls)
    }

    @Test
    fun `no cache and no network propagates error`() = runTest {
        val api = CountingApi(fail = true)
        assertTrue(repo(api) { LocalDate.of(2026, 11, 17) }.getConfiguracion().isFailure)
    }
}
