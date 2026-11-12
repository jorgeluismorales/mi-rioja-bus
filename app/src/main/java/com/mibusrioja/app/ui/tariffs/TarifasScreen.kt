package com.mibusrioja.app.ui.tariffs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mibusrioja.app.domain.model.TarifaItem
import com.mibusrioja.app.ui.theme.Background
import com.mibusrioja.app.ui.theme.DividerColor
import com.mibusrioja.app.ui.theme.ErrorRed
import com.mibusrioja.app.ui.theme.GlassBackground
import com.mibusrioja.app.ui.theme.GlassBorder
import com.mibusrioja.app.ui.theme.InfoBlue
import com.mibusrioja.app.ui.theme.RiojaAmber
import com.mibusrioja.app.ui.theme.RiojaOrange
import com.mibusrioja.app.ui.theme.RiojaOrangeDark
import com.mibusrioja.app.ui.theme.SuccessGreen
import com.mibusrioja.app.ui.theme.SurfaceCard
import com.mibusrioja.app.ui.theme.SurfaceDark
import com.mibusrioja.app.ui.theme.SurfaceVariant
import com.mibusrioja.app.ui.theme.TextMuted
import com.mibusrioja.app.ui.theme.TextPrimary
import com.mibusrioja.app.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarifasScreen(
    modifier: Modifier = Modifier,
    viewModel: TarifasViewModel = hiltViewModel()
) {
    val tarifas by viewModel.tarifas.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullToRefreshState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Background
    ) {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header con iconos e información oficial
                item {
                    TarifasHeader()
                }

                // Error Banner si ocurrió un fallo
                if (error != null) {
                    item {
                        TarifasErrorCard(
                            errorMessage = error.orEmpty(),
                            onRetry = { viewModel.loadTarifas() }
                        )
                    }
                }

                // Skeleton Loader cuando carga por primera vez
                if (isLoading && tarifas.isEmpty()) {
                    item {
                        TarifasSkeletonList()
                    }
                } else if (tarifas.isNotEmpty()) {
                    // Separamos la tarjeta destacada ("Usuario Común") del resto
                    val featured = tarifas.firstOrNull { isFeaturedTarifa(it.categoria) }
                    val remaining = tarifas.filter { it != featured }

                    if (featured != null) {
                        item {
                            TarifaFeaturedCard(tarifa = featured)
                        }
                    }

                    if (remaining.isNotEmpty()) {
                        item {
                            Text(
                                text = "Otras categorías y beneficios",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(remaining, key = { it.categoria }) { tarifa ->
                            TarifaCard(tarifa = tarifa)
                        }
                    }

                    // Nota legal informativa al pie
                    item {
                        TarifasFooterNote()
                    }
                } else if (!isLoading && error == null) {
                    item {
                        TarifasEmptyState(onRefresh = { viewModel.refresh() })
                    }
                }
            }
        }
    }
}

/**
 * Cabecera con íconos de colectivo y ticket, además del título principal.
 */
@Composable
fun TarifasHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(RiojaOrange, RiojaOrangeDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ConfirmationNumber,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tarifas vigentes",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Rounded.DirectionsBus,
                        contentDescription = null,
                        tint = RiojaOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Valores oficiales del sistema de transporte urbano",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Badge informativo con gradiente suave
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = RiojaAmber,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Tarifas actualizadas en tiempo real desde los servidores de Mi Bus Rioja.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Tarjeta destacada y de mayor tamaño para la tarifa principal (ej: Usuario Común).
 */
@Composable
fun TarifaFeaturedCard(tarifa: TarifaItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF24140D),
                        SurfaceCard,
                        SurfaceVariant
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        RiojaOrange,
                        RiojaOrange.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Franja vertical destacada de acento
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(RiojaOrange, RiojaAmber)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Etiqueta superior TARIFA GENERAL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RiojaOrange.copy(alpha = 0.2f))
                            .border(1.dp, RiojaOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = RiojaOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "TARIFA HABITUAL",
                                color = RiojaOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        text = "Boleto Plano",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tarifa.categoria,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Válido para todas las líneas y recorridos urbanos",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = formatTarifaPrice(tarifa.precio),
                        color = RiojaOrange,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta estándar de tarifa: fondo oscuro, borde izquierdo naranja, categoría a la izquierda
 * y precio grande a la derecha. Soporta badges especiales (PASE LIBRE, Boleto Estudiantil, etc.).
 */
@Composable
fun TarifaCard(tarifa: TarifaItem) {
    val paseLibre = isPaseLibre(tarifa.categoria, tarifa.precio)
    val estudiantil = isEstudiantil(tarifa.categoria)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SurfaceCard,
                        SurfaceDark
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (paseLibre) SuccessGreen.copy(alpha = 0.35f) else GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Franja lateral izquierda en RiojaOrange o verde para pase libre
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        if (paseLibre) SuccessGreen else RiojaOrange
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Categoría y badges
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tarifa.categoria,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (paseLibre) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TarifaBadge(
                            text = "PASE LIBRE (100% BONIFICADO)",
                            icon = Icons.Rounded.Accessible,
                            color = SuccessGreen
                        )
                    } else if (estudiantil) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TarifaBadge(
                            text = "BOLETO ESTUDIANTIL",
                            icon = Icons.Rounded.School,
                            color = InfoBlue
                        )
                    }
                }

                // Precio destacado a la derecha
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatTarifaPrice(tarifa.precio),
                        color = if (paseLibre) SuccessGreen else TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )

                    if (paseLibre) {
                        Text(
                            text = "Sin cargo",
                            color = SuccessGreen.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip o insignia para categorías especiales con icono temático.
 */
@Composable
private fun TarifaBadge(
    text: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

/**
 * Tarjeta de error con botón de reintento.
 */
@Composable
fun TarifasErrorCard(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ErrorRed.copy(alpha = 0.12f))
            .border(1.dp, ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = errorMessage,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RiojaOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Reintentar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Estado vacío cuando no hay tarifas configuradas en el servidor.
 */
@Composable
fun TarifasEmptyState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ConfirmationNumber,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "No hay tarifas disponibles por el momento",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Actualizar", color = TextPrimary)
        }
    }
}

/**
 * Skeleton animado con efecto Shimmer para indicar carga inicial fluida.
 */
@Composable
fun TarifasSkeletonList() {
    val transition = rememberInfiniteTransition(label = "shimmerSkeleton")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerFloat"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            SurfaceCard,
            SurfaceVariant,
            SurfaceCard
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Tarjeta skeleton destacada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(shimmerBrush)
        )

        // 3 Tarjetas skeleton estándar
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

/**
 * Nota informativa final sobre transbordo y reglamentación.
 */
@Composable
fun TarifasFooterNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "El transbordo entre líneas conectadas es bonificado dentro del plazo de 60 minutos con la misma tarjeta registrada.",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

// ── Helpers de formato y clasificación ─────────────────────────────────────────

private fun isFeaturedTarifa(categoria: String): Boolean {
    val cat = categoria.lowercase(Locale.ROOT)
    return cat.contains("común") || cat.contains("comun") || cat.contains("general") || cat.contains("urbano")
}

private fun isPaseLibre(categoria: String, precio: String): Boolean {
    val cat = categoria.lowercase(Locale.ROOT)
    val pr = precio.lowercase(Locale.ROOT)
    return cat.contains("discapacidad") ||
            cat.contains("pase libre") ||
            pr.contains("libre") ||
            pr.contains("gratis") ||
            pr == "0" ||
            pr == "$0" ||
            pr == "$ 0" ||
            pr == "$0,00"
}

private fun isEstudiantil(categoria: String): Boolean {
    val cat = categoria.lowercase(Locale.ROOT)
    return cat.contains("estudiantil") || cat.contains("escolar") || cat.contains("universitario")
}

private fun formatTarifaPrice(precio: String): String {
    val clean = precio.trim()
    return when {
        clean.isEmpty() -> "$ 0,00"
        clean.startsWith("$") -> clean
        clean.all { it.isDigit() || it == '.' || it == ',' } -> "$ $clean"
        else -> clean
    }
}
