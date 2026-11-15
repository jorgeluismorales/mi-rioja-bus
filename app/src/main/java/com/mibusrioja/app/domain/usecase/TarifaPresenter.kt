package com.mibusrioja.app.domain.usecase

import com.mibusrioja.app.domain.model.TarifaItem
import java.util.Locale

enum class TarifaKind { ESTANDAR, DESCUENTO, GRATIS }

data class TarifaDisplay(
    val categoria: String,
    val precioFinal: String,
    val kind: TarifaKind,
    val etiqueta: String? = null,
    val precioBase: String? = null
)

fun parseArgPrice(raw: String): Double? {
    val cleaned = raw.replace("$", "").replace(" ", "").trim()
    if (cleaned.isEmpty()) return null
    val normalized = when {
        cleaned.contains('.') && cleaned.contains(',') -> cleaned.replace(".", "").replace(",", ".")
        cleaned.contains(',') -> cleaned.replace(",", ".")
        else -> cleaned
    }
    return normalized.toDoubleOrNull()
}

fun formatArgPrice(value: Double): String {
    val grouped = String.format(Locale.US, "%,.2f", value)
    return "$" + grouped.replace(',', '#').replace('.', ',').replace('#', '.')
}

fun buildTarifaDisplays(items: List<TarifaItem>): List<TarifaDisplay> {
    val base = items.firstOrNull { it.categoria.contains("común", ignoreCase = true) || it.categoria.contains("comun", ignoreCase = true) }
        ?.let { parseArgPrice(it.precio) }
        ?: items.firstNotNullOfOrNull { parseArgPrice(it.precio) }

    return items.mapNotNull { item ->
        val categoria = item.categoria.trim().removeSuffix(":").trim()
        val raw = item.precio.trim()
        val percentMatch = Regex("(\\d+)\\s*%").find(raw)
        when {
            percentMatch != null -> {
                val n = percentMatch.groupValues[1].toInt()
                TarifaDisplay(
                    categoria = categoria,
                    precioFinal = if (base != null) formatArgPrice(base * (1 - n / 100.0)) else raw,
                    kind = TarifaKind.DESCUENTO,
                    etiqueta = "$n% Dto.",
                    precioBase = base?.let { formatArgPrice(it) }
                )
            }
            raw.contains("pase libre", ignoreCase = true) ||
                raw.contains("gratis", ignoreCase = true) ||
                raw.contains("libre", ignoreCase = true) -> TarifaDisplay(
                categoria = categoria,
                precioFinal = "Pase Libre",
                kind = TarifaKind.GRATIS,
                etiqueta = null
            )
            else -> {
                val parsed = parseArgPrice(raw)
                if (parsed != null && parsed == 0.0) null
                else TarifaDisplay(
                    categoria = categoria,
                    precioFinal = parsed?.let { formatArgPrice(it) } ?: raw,
                    kind = TarifaKind.ESTANDAR
                )
            }
        }
    }
}
