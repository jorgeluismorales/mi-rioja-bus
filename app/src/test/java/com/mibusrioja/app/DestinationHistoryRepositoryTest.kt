package com.mibusrioja.app

import com.mibusrioja.app.data.local.FrequentDestinationDao
import com.mibusrioja.app.data.local.FrequentDestinationEntity
import com.mibusrioja.app.data.repository.DestinationHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationHistoryRepositoryTest {

    private class FakeDao : FrequentDestinationDao {
        val rows = mutableMapOf<String, FrequentDestinationEntity>()
        override fun getTop(limit: Int): Flow<List<FrequentDestinationEntity>> = flowOf(rows.values.toList())
        override suspend fun find(key: String) = rows[key]
        override suspend fun upsert(e: FrequentDestinationEntity) { rows[e.key] = e }
        override suspend fun delete(key: String) { rows.remove(key) }
    }

    private val dao = FakeDao()
    private val repo = DestinationHistoryRepository(dao)

    @Test
    fun `relocate mueve el destino guardado y suma los usos del que ya existia`() = runTest {
        repo.registerVisit("UNLaR", "", -29.434720, -66.871530)
        repo.registerVisit("UNLaR", "", -29.434720, -66.871530)
        repo.registerVisit("UNLaR", "", -29.4305503, -66.8705263)

        repo.relocate("-29.4347,-66.8715", -29.4305503, -66.8705263)

        assertNull(dao.rows["-29.4347,-66.8715"])
        val moved = dao.rows.values.single()
        assertEquals("-29.4306,-66.8705", moved.key)
        assertEquals(-29.4305503, moved.lat, 0.0)
        assertEquals(3, moved.vecesUsado)
    }

    @Test
    fun `relocate a las mismas coordenadas no cambia nada`() = runTest {
        repo.registerVisit("UNLaR", "", -29.4305503, -66.8705263)
        repo.relocate("-29.4306,-66.8705", -29.4305503, -66.8705263)
        assertEquals(1, dao.rows.values.single().vecesUsado)
    }
}
