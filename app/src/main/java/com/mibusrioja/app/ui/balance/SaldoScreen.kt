package com.mibusrioja.app.ui.balance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mibusrioja.app.domain.model.Movimiento
import com.mibusrioja.app.domain.model.Saldo
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

@Composable
fun SaldoScreen(
    modifier: Modifier = Modifier,
    viewModel: SaldoViewModel = hiltViewModel()
) {
    val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle()
    val saldo by viewModel.saldo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Ilustración interactiva de tarjeta de transporte
            item {
                SaldoCardIllustration(
                    cardNumber = cardNumber,
                    saldo = saldo
                )
            }

            // Formulario de consulta (Input + Nota + Botón)
            item {
                SaldoInputCard(
                    cardNumber = cardNumber,
                    isLoading = isLoading,
                    onCardNumberChange = { viewModel.updateCardNumber(it) },
                    onConsultarClick = {
                        keyboardController?.hide()
                        viewModel.checkBalance()
                    }
                )
            }

            // Error Banner
            if (error != null) {
                item {
                    SaldoErrorBanner(
                        errorMessage = error.orEmpty(),
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            // Resultados de saldo con animación suave de entrada
            item {
                AnimatedVisibility(
                    visible = saldo != null,
                    enter = slideInVertically(initialOffsetY = { 80 }) + fadeIn() + expandVertically(),
                    exit = slideOutVertically() + fadeOut() + shrinkVertically()
                ) {
                    saldo?.let { s ->
                        SaldoResultSection(saldo = s)
                    }
                }
            }

            // Estado inicial vacío con consejos informativos
            if (saldo == null && !isLoading && error == null) {
                item {
                    SaldoEmptyTips()
                }
            }
        }
    }
}

/**
 * Ilustración gráfica de la tarjeta de transporte Mi Bus Rioja / Red Bus.
 */
@Composable
fun SaldoCardIllustration(
    cardNumber: String,
    saldo: Saldo?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF26120B),
                        SurfaceCard,
                        Color(0xFF161F36)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        RiojaOrange.copy(alpha = 0.6f),
                        GlassBorder,
                        RiojaOrange.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Fila superior: Logo y Contactless
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(RiojaOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsBus,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Mi Bus Rioja",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Red Bus La Rioja",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.Contactless,
                    contentDescription = null,
                    tint = RiojaAmber,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Chip electrónico de la tarjeta física
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SimCard,
                    contentDescription = null,
                    tint = RiojaAmber.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Fila inferior: Número de tarjeta y estado
            Column {
                val formattedNumber = if (cardNumber.isNotBlank()) {
                    formatCardNumberDisplay(cardNumber)
                } else {
                    "•••• •••• •••• ••••"
                }

                Text(
                    text = formattedNumber,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = saldo?.tipo?.uppercase(Locale.ROOT) ?: "TARJETA INTELIGENTE",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    )

                    if (saldo != null) {
                        Text(
                            text = formatArgCurrency(saldo.saldo),
                            color = if (saldo.saldo >= 0) SuccessGreen else ErrorRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "TRANSPORTE URBANO",
                            color = TextMuted,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formulario de entrada: campo de número de tarjeta, instrucciones y botón de acción.
 */
@Composable
fun SaldoInputCard(
    cardNumber: String,
    isLoading: Boolean,
    onCardNumberChange: (String) -> Unit,
    onConsultarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Consultar saldo de tarjeta",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Input estilizado para tarjeta
        OutlinedTextField(
            value = cardNumber,
            onValueChange = onCardNumberChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(text = "5679782", color = TextMuted)
            },
            label = {
                Text(text = "Número de tarjeta")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    tint = RiojaOrange
                )
            },
            trailingIcon = {
                if (cardNumber.isNotEmpty()) {
                    IconButton(onClick = { onCardNumberChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = "Borrar",
                            tint = TextMuted
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onConsultarClick() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = RiojaOrange,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = RiojaOrange,
                unfocusedLabelColor = TextSecondary,
                cursorColor = RiojaOrange
            )
        )

        // Nota aclaratoria
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = RiojaAmber,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Ingresá el número sin espacios ni puntos",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Botón principal Consultar Saldo
        Button(
            onClick = onConsultarClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RiojaOrange,
                contentColor = Color.White,
                disabledContainerColor = RiojaOrange.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Consultando...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Consultar saldo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Sección animada con el saldo resultante y la lista de últimos movimientos.
 */
@Composable
fun SaldoResultSection(saldo: Saldo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Tarjeta grande de Saldo disponible
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            SurfaceCard,
                            Color(0xFF14243B)
                        )
                    )
                )
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tarjeta N° ${saldo.nroTarjeta}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Badge Tipo de tarjeta
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RiojaOrange.copy(alpha = 0.2f))
                            .border(1.dp, RiojaOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = saldo.tipo.ifBlank { "Común" },
                            color = RiojaOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Saldo disponible",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                Text(
                    text = formatArgCurrency(saldo.saldo),
                    color = if (saldo.saldo >= 0) SuccessGreen else ErrorRed,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                if (saldo.saldo < 0) {
                    Text(
                        text = "Saldo de emergencia utilizado",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Sección Últimos movimientos
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = RiojaOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Últimos movimientos",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                if (saldo.movimientos.isNotEmpty()) {
                    Text(
                        text = "(${saldo.movimientos.size})",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            if (saldo.movimientos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se registran movimientos recientes para esta tarjeta.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    saldo.movimientos.forEach { movimiento ->
                        MovimientoCard(movimiento = movimiento)
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual de movimiento (débito de viaje o recarga acreditada).
 */
@Composable
fun MovimientoCard(movimiento: Movimiento) {
    val isCarga = movimiento.monto > 0 || movimiento.tipo.contains("carga", ignoreCase = true)
    val accentColor = if (isCarga) SuccessGreen else ErrorRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ícono del movimiento
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCarga) Icons.Rounded.ArrowUpward else Icons.Rounded.DirectionsBus,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Descripción y fecha
                Column(modifier = Modifier.weight(1f)) {
                    val title = when {
                        !movimiento.linea.isNullOrBlank() -> "Línea ${movimiento.linea}"
                        !movimiento.descripcion.isNullOrBlank() -> movimiento.descripcion
                        isCarga -> "Recarga de saldo"
                        else -> movimiento.tipo.ifBlank { "Viaje en colectivo" }
                    }

                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = movimiento.fecha,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Monto formateado con signo + o -
            val amountText = if (isCarga) {
                "+ ${formatArgCurrency(abs(movimiento.monto))}"
            } else {
                "- ${formatArgCurrency(abs(movimiento.monto))}"
            }

            Text(
                text = amountText,
                color = accentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Banner de error con opción para descartar.
 */
@Composable
fun SaldoErrorBanner(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ErrorRed.copy(alpha = 0.12f))
            .border(1.dp, ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = errorMessage,
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cancel,
                    contentDescription = "Cerrar",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Guía inicial de ayuda antes de realizar la consulta.
 */
@Composable
fun SaldoEmptyTips() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Payments,
                contentDescription = null,
                tint = RiojaOrange,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "¿Dónde encontrar tu número de tarjeta?",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "• Encontrás el código de 6 a 8 dígitos impreso en el frente o dorso de tu tarjeta plástica Mi Bus Rioja / Sirve.\n• La consulta es 100% gratuita y no requiere registrar una cuenta personal.\n• Podrás ver tus fondos actualizados y el historial de débitos y recargas.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatCardNumberDisplay(raw: String): String {
    return raw.chunked(4).joinToString(" ")
}

private fun formatArgCurrency(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale("es", "AR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("$ #,##0.00", symbols)
    return formatter.format(amount)
}
