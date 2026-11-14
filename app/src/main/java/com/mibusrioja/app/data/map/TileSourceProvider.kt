package com.mibusrioja.app.data.map

import com.mibusrioja.app.domain.model.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resuelve qué plantilla de tiles usar: prueba la primaria con un tile real
 * y si responde algo que no sea una imagen, cae a la secundaria.
 * El resultado se cachea durante la sesión.
 */
@Singleton
class TileSourceProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val FALLBACK_OSM_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    }

    @Volatile private var resolvedUrl: String? = null

    suspend fun resolve(config: AppConfig): String = withContext(Dispatchers.IO) {
        resolvedUrl?.let { return@withContext it }

        val primary = config.urlPrincipalMapas.substringBefore('?')
        val secondary = config.urlSecundariaMapas.substringBefore('?')

        val chosen = if (primary.contains("{z}") && probeTile(primary)) {
            primary
        } else {
            secondary.takeIf { it.contains("{z}") && it.isNotBlank() }
                ?: FALLBACK_OSM_URL
        }

        resolvedUrl = chosen
        chosen
    }

    private fun probeTile(template: String): Boolean {
        return try {
            val url = template
                .replace("{z}", "13")
                .replace("{x}", "2574")
                .replace("{y}", "4815")
            val client = okHttpClient.newBuilder()
                .followRedirects(false)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val contentType = resp.header("Content-Type").orEmpty()
                resp.code == 200 && contentType.startsWith("image/")
            }
        } catch (_: Exception) {
            false
        }
    }
}
