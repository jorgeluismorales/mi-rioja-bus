package com.mibusrioja.app

import com.mibusrioja.app.domain.model.TiempoEstimado
import org.junit.Assert.assertEquals
import org.junit.Test

class TiempoEstimadoTest {

    @Test
    fun `formatea los tiempos correctamente`() {
        assertEquals("Sin datos", TiempoEstimado(null, null).formatearTiempo())
        assertEquals("Menos de 1 min", TiempoEstimado(45, "1").formatearTiempo())
        assertEquals("Menos de 1 min", TiempoEstimado(60, "1").formatearTiempo())
        assertEquals("1 minuto", TiempoEstimado(90, "1").formatearTiempo())
        assertEquals("10 minutos", TiempoEstimado(600, "1").formatearTiempo())
    }
}
