package com.mibusrioja.app.data.local

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.time.LocalDate

/**
 * Caché persistente de respuestas de la API con vigencia de un día calendario.
 * Permite consultar los endpoints estáticos (config/tarifas, líneas, recorridos)
 * una sola vez por día y seguir funcionando sin red con los datos guardados.
 */
interface ApiCache {
    class Entry<T>(val data: T, val fresh: Boolean)

    suspend fun <T> read(key: String, type: Class<T>): Entry<T>?
    suspend fun <T> write(key: String, type: Class<T>, value: T)
}

/** Caché deshabilitada (tests / fallback). */
object NoApiCache : ApiCache {
    override suspend fun <T> read(key: String, type: Class<T>): ApiCache.Entry<T>? = null
    override suspend fun <T> write(key: String, type: Class<T>, value: T) {}
}

@JsonClass(generateAdapter = true)
class CachedEnvelope<T>(val date: String, val data: T)

class DailyApiCache(
    private val dir: File,
    private val moshi: Moshi,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ApiCache {

    private fun file(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9_.-]"), "_") + ".json")

    private fun <T> adapter(type: Class<T>) =
        moshi.adapter<CachedEnvelope<T>>(Types.newParameterizedType(CachedEnvelope::class.java, type))

    override suspend fun <T> read(key: String, type: Class<T>): ApiCache.Entry<T>? {
        val f = file(key)
        if (!f.exists()) return null
        return runCatching {
            val env = adapter(type).fromJson(f.readText()) ?: return null
            ApiCache.Entry(env.data, fresh = env.date == today().toString())
        }.getOrElse { f.delete(); null }
    }

    override suspend fun <T> write(key: String, type: Class<T>, value: T) {
        runCatching {
            dir.mkdirs()
            val tmp = File(dir, file(key).name + ".tmp")
            tmp.writeText(adapter(type).toJson(CachedEnvelope(today().toString(), value)))
            tmp.renameTo(file(key))
        }
    }
}
