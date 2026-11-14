package com.mibusrioja.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import com.mibusrioja.app.R
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
 * Ícono de colectivo en tiempo real: círculo naranja con el pictograma de bus siempre derecho
 * y una flecha exterior rotada según [orientacionGrados] (0° = norte, sentido horario).
 * El marcador no se rota: la orientación queda "horneada" en el bitmap para que el bus sea legible.
 * Los colectivos con rampa se pintan en azul y llevan una insignia de accesibilidad.
 */
internal fun createBusMarkerDrawable(context: Context, orientacionGrados: Float, tieneRampa: Boolean = false): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (64 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val radius = 15f * density

    // Flecha de dirección (rotada alrededor del centro)
    canvas.save()
    canvas.rotate(orientacionGrados, cx, cy)
    val arrowTip = cy - radius - 14f * density
    val arrowBase = cy - radius - 3f * density
    val arrowPath = Path().apply {
        moveTo(cx, arrowTip)
        lineTo(cx - 9f * density, arrowBase)
        lineTo(cx, arrowBase - 3f * density)
        lineTo(cx + 9f * density, arrowBase)
        close()
    }
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    canvas.drawPath(arrowPath, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f * density
    paint.strokeJoin = Paint.Join.ROUND
    paint.color = Color(0xFF111827).toArgb()
    canvas.drawPath(arrowPath, paint)
    canvas.restore()

    // Sombra + borde blanco + círculo naranja
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.parseColor("#55000000")
    canvas.drawCircle(cx, cy + 1.5f * density, radius + 2f * density, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, radius + 2f * density, paint)
    paint.color = (if (tieneRampa) Color(0xFF3B82F6) else Color(0xFFFF6B35)).toArgb()
    canvas.drawCircle(cx, cy, radius, paint)

    // Pictograma de colectivo (Material "directions_bus"), siempre derecho
    val icon = ContextCompat.getDrawable(context, R.drawable.ic_bus_marker)?.mutate()
    if (icon != null) {
        val half = (11f * density).toInt()
        icon.setBounds((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
        icon.setTint(android.graphics.Color.WHITE)
        icon.draw(canvas)
    }

    // Insignia de accesibilidad (abajo a la derecha) para colectivos con rampa
    if (tieneRampa) {
        val bx = cx + radius * 0.75f
        val by = cy + radius * 0.75f
        val badgeR = 7f * density
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(bx, by, badgeR + 1.5f * density, paint)
        paint.color = Color(0xFF10B981).toArgb()
        canvas.drawCircle(bx, by, badgeR, paint)
        val badge = ContextCompat.getDrawable(context, R.drawable.ic_accessible_badge)?.mutate()
        if (badge != null) {
            val bh = (5f * density).toInt()
            badge.setBounds((bx - bh).toInt(), (by - bh).toInt(), (bx + bh).toInt(), (by + bh).toInt())
            badge.setTint(android.graphics.Color.WHITE)
            badge.draw(canvas)
        }
    }

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
