package com.mibusrioja.app

import com.mibusrioja.app.data.api.BrokenAccents
import com.mibusrioja.app.data.api.model.RutaLineaResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BrokenAccentsTest {

    @Test
    fun `repara las palabras de las paradas`() {
        assertEquals("P005 Islas Decepción", BrokenAccents.repair("P005 Islas Decepci\uFFFDn"))
        assertEquals("P002 Orzán y Cesar Corzo", BrokenAccents.repair("P002 Orz\uFFFDn y Cesar Corzo"))
        assertEquals(
            "P042 Av. Santa Rosa y Ñoñolo Albaracin",
            BrokenAccents.repair("P042 Av. Santa Rosa y \uFFFDo\uFFFDolo Albaracin")
        )
        assertEquals(
            "P043 Rio las peñas y Rio Retamal",
            BrokenAccents.repair("P043 Rio las pe\uFFFDas y Rio Retamal")
        )
    }

    @Test
    fun `numero de calle o escuela usa el signo de grado`() {
        assertEquals("P028 C.E.F N°5", BrokenAccents.repair("P028 C.E.F N\uFFFD5"))
        assertEquals("P040 Calle Pública N° 1057", BrokenAccents.repair("P040 Calle P\uFFFDblica N\uFFFD 1057"))
    }

    @Test
    fun `respeta mayusculas y deja intacto el texto sano`() {
        assertEquals("AVENIDA JUAN DOMINGO PERÓN", BrokenAccents.repair("AVENIDA JUAN DOMINGO PER\uFFFDN"))
        assertEquals("Pensión", BrokenAccents.repair("Pensi\uFFFDn"))
        assertEquals("Avenida Perón", BrokenAccents.repair("Avenida Perón"))
        assertEquals("Xyz\uFFFDw", BrokenAccents.repair("Xyz\uFFFDw"))
    }

    @Test
    fun `todas las paradas rotas de la api quedan reparadas`() {
        for (name in LIVE_BROKEN_NAMES) {
            val fixed = BrokenAccents.repair(name)
            assertFalse("$name -> $fixed", '\uFFFD' in fixed)
        }
    }

    @Test
    fun `moshi repara los nombres al parsear el recorrido`() {
        val ruta = TestFixtures.parse<RutaLineaResponse>("rutaLinea_11.json")
        val names = ruta.nodos.mapNotNull { it.descripcionParada }
        assertFalse(names.any { '\uFFFD' in it })
        assert("P005 Islas Decepción" in names)
        assertNull(TestFixtures.parseJson<RutaLineaResponse>("""{"nodos":[{"descripcionParada":null}]}""").nodos.first().descripcionParada)
    }

    private companion object {
        /** Nombres con U+FFFD que devolvía /rest/rutaLinea para todas las líneas. */
        val LIVE_BROKEN_NAMES = listOf(
            "Avenida San Nicol\uFFFDs de Bari Este",
            "Grido, Avenida San Nicol\uFFFDs de Bari",
            "P001 Gobernador Eduardo Fern\uFFFDndez Validez",
            "P001 Rio las Pe\uFFFDas y Rio Retamal",
            "P002 Orz\uFFFDn y Cesar Corzo",
            "P003 Carlos Pe\uFFFDaloza y Tomas Ortiz",
            "P004 Islas Decepci\uFFFDn",
            "P005 Islas Decepci\uFFFDn",
            "P006 Islas Decepci\uFFFDn",
            "P007 Islas Decepci\uFFFDn",
            "P008 Guaran\uFFFDes",
            "P010 Av. Santa Rosa y Av. M\uFFFDrtires de la Dictadura",
            "P015 Joaqu\uFFFDn V. Gonz\uFFFDlez",
            "P015 M\uFFFDlaga",
            "P018 Centro Policial de Educaci\uFFFDn F\uFFFDsica",
            "P019 Avenida Carlos Sa\uFFFDl Menem",
            "P021 E.P.E.T N\uFFFD2, Avenida Juan Domingo Per\uFFFDn",
            "P021 La Basija, Avenida Juan Domingo Per\uFFFDn",
            "P022 Avenida Juan Domingo Per\uFFFDn",
            "P022 E.P.E.T N\uFFFD2 ",
            "P023 146, Avenida Juan Domingo Per\uFFFDn",
            "P026 Centro Policial de Educaci\uFFFDn F\uFFFDsica",
            "P027 Avenida Carlos Sa\uFFFDl Menem",
            "P028 C.E.F N\uFFFD5",
            "P029 Av. 2 de Abril y Jos\uFFFD C\uFFFDrdoba",
            "P029 M\uFFFDlaga",
            "P029 Secretar\uFFFDa de Turismo, Avenida Ortiz de Ocampo",
            "P037 Las vicu\uFFFDas y Av. Luis Martines de la fuente",
            "P038 Las Vicu\uFFFDas y Pucar\uFFFD",
            "P039 Las Vicu\uFFFDas y Av. Santa Rosa",
            "P040 Calle P\uFFFDblica N\uFFFD 1057",
            "P042 Av. Santa Rosa y \uFFFDo\uFFFDolo Albaracin",
            "P042 Clodulfa Orz\uFFFDn",
            "P042 Rio las Pe\uFFFDas",
            "P043 Clodulfa Orz\uFFFDn",
            "P043 Rio las pe\uFFFDas y Rio Retamal",
            "P046 Calle P\uFFFDblica N\uFFFD 1057",
            "P047 Calle P\uFFFDblica N\uFFFD 1056",
            "P048 Carlos Pe\uFFFDaloza y Tomas Ortiz",
            "P067 C.E.F N\uFFFD5",
            "P068 Avenida Carlos Sa\uFFFDl Menem",
            "P069 Avenida Carlos Sa\uFFFDl Menem",
            "P071 Avenida Juan Domingo Per\uFFFDn",
            "P072 E.P.E.T N\uFFFD2, Avenida Juan Domingo Per\uFFFDn",
            "P073 148, Avenida Juan Domingo Per\uFFFDn",
            "P085 Gobernador Eduardo Fern\uFFFDndez Validez",
            "P090 Guaran\uFFFDes",
            "Ruta Provincial N\uFFFD5, La Rioja"
        )
    }
}
