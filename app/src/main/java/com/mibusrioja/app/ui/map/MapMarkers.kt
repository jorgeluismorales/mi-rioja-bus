package com.mibusrioja.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Genera dinámicamente un ícono circular blanco con borde RiojaOrange para las paradas.
 */
internal fun createStopMarkerDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (22 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Sombra sutil
    paint.color = android.graphics.Color.parseColor("#44000000")
    paint.style = Paint.Style.FILL
    canvas.drawCircle(cx, cy + 1f * density, 9f * density, paint)

    // Aro exterior naranja
    paint.color = Color(0xFFFF6B35).toArgb()
    canvas.drawCircle(cx, cy, 8.5f * density, paint)

    // Círculo blanco central
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, 6.5f * density, paint)

    // Punto central oscuro
    paint.color = Color(0xFF111827).toArgb()
    canvas.drawCircle(cx, cy, 3f * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Genera dinámicamente un ícono de colectivo en tiempo real con indicador de orientación.
 */
internal fun createBusMarkerDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (44 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val radius = 14f * density

    // Puntero triangular de orientación hacia el norte (rotado luego con bus.orientacion)
    paint.color = Color(0xFFCC4D1A).toArgb()
    paint.style = Paint.Style.FILL
    val pointerPath = Path().apply {
        moveTo(cx, 2f * density)
        lineTo(cx - 5f * density, cy - radius + 2f * density)
        lineTo(cx + 5f * density, cy - radius + 2f * density)
        close()
    }
    canvas.drawPath(pointerPath, paint)

    // Borde blanco exterior
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, radius + 2f * density, paint)

    // Círculo principal naranja
    paint.color = Color(0xFFFF6B35).toArgb()
    canvas.drawCircle(cx, cy, radius, paint)

    // Silueta de colectivo blanca en el centro
    paint.color = android.graphics.Color.WHITE
    val busW = 12f * density
    val busH = 16f * density
    val busRect = RectF(
        cx - busW / 2f,
        cy - busH / 2f,
        cx + busW / 2f,
        cy + busH / 2f
    )
    canvas.drawRoundRect(busRect, 2.5f * density, 2.5f * density, paint)

    // Parabrisas oscuro
    paint.color = Color(0xFF111827).toArgb()
    val winRect = RectF(
        cx - busW / 2f + 1.5f * density,
        cy - busH / 2f + 1.5f * density,
        cx + busW / 2f - 1.5f * density,
        cy - busH / 2f + 5.5f * density
    )
    canvas.drawRoundRect(winRect, 1.5f * density, 1.5f * density, paint)

    // Luces delanteras ámbar
    paint.color = Color(0xFFFFA726).toArgb()
    canvas.drawCircle(cx - 3.5f * density, cy + busH / 2f - 2f * density, 1.2f * density, paint)
    canvas.drawCircle(cx + 3.5f * density, cy + busH / 2f - 2f * density, 1.2f * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Punto circular simple relleno con borde blanco, para origen/destino del planificador.
 */
internal fun createPointMarkerDrawable(context: Context, color: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (20 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = sizePx / 2f
    val cy = sizePx / 2f

    paint.color = android.graphics.Color.parseColor("#44000000")
    paint.style = Paint.Style.FILL
    canvas.drawCircle(cx, cy + 1f * density, 8f * density, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, 7.5f * density, paint)

    paint.color = color
    canvas.drawCircle(cx, cy, 6f * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Genera un ícono circular RiojaOrange con anillo blanco y el conteo de colectivos agrupados.
 */
internal fun createClusterDrawable(context: Context, count: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (40 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val radius = 16f * density

    paint.color = android.graphics.Color.parseColor("#44000000")
    paint.style = Paint.Style.FILL
    canvas.drawCircle(cx, cy + 1f * density, radius, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, radius, paint)

    paint.color = Color(0xFFFF6B35).toArgb()
    canvas.drawCircle(cx, cy, radius - 2f * density, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.isFakeBoldText = true
    paint.textSize = if (count >= 10) 14f * density else 16f * density
    val yOffset = (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(count.toString(), cx, cy - yOffset, paint)

    return BitmapDrawable(context.resources, bitmap)
}
