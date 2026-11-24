package com.mibusrioja.app.data.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * La API de MiRedBus manda los acentos y la ñ ya reemplazados por U+FFFD
 * ("P005 Islas Decepci\uFFFDn"), así que la letra original se perdió en el servidor.
 * Se reconstruye con un diccionario de palabras de las paradas de La Rioja.
 */
object BrokenAccents {

    private const val BAD = '\uFFFD'

    private val WORDS = listOf(
        "Álvarez", "Ángel", "Benítez", "Bolívar", "Córdoba", "Decepción", "Díaz",
        "Educación", "Fernández", "Física", "Gómez", "González", "Guaraníes", "Güemes",
        "Hernández", "Inés", "Jesús", "Jiménez", "Joaquín", "José", "López", "Málaga",
        "María", "Martín", "Martínez", "Mártires", "Muñoz", "Nación", "Nicolás", "Niño",
        "Núñez", "Ñoñolo", "Orzán", "Peña", "Peñaloza", "Peñas", "Pérez", "Perón",
        "Pública", "Pucará", "Ramón", "Rodríguez", "Sáenz", "Sánchez", "Saúl",
        "Secretaría", "Señor", "Señora", "Unión", "Vicuña", "Vicuñas"
    )

    private val ACCENTED = Regex("[^\\x00-\\x7F]")
    private val BY_BROKEN: Map<String, String> =
        WORDS.associateBy { it.replace(ACCENTED, BAD.toString()).lowercase() }

    private val TOKEN = Regex("[\\p{L}$BAD]*$BAD[\\p{L}$BAD]*")

    fun repair(text: String): String {
        if (BAD !in text) return text
        return TOKEN.replace(text) { m -> repairWord(m.value) }
    }

    private fun repairWord(token: String): String {
        if (token.equals("N$BAD", ignoreCase = true)) return "${token[0]}°"
        val lower = token.lowercase()
        val word = BY_BROKEN[lower]
            ?: when {
                lower.endsWith("ci${BAD}n") || lower.endsWith("si${BAD}n") ->
                    token.dropLast(2) + "ón"
                else -> return token
            }
        val letters = token.filter { it != BAD }
        return when {
            letters.length > 1 && letters.all { it.isUpperCase() } -> word.uppercase()
            token[0].isLowerCase() -> word.replaceFirstChar { it.lowercase() }
            else -> word
        }
    }

    val stringAdapter: JsonAdapter<String> = object : JsonAdapter<String>() {
        override fun fromJson(reader: JsonReader): String = repair(reader.nextString())
        override fun toJson(writer: JsonWriter, value: String?) {
            writer.value(value)
        }
    }.nullSafe()
}
