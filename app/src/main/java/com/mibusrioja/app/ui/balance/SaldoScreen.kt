package com.mibusrioja.app.ui.balance

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mibusrioja.app.domain.model.Saldo
import com.mibusrioja.app.ui.components.AppBottomSheet
import com.mibusrioja.app.ui.components.AppCard
import com.mibusrioja.app.ui.components.AppTextField
import com.mibusrioja.app.ui.components.Dimens
import com.mibusrioja.app.ui.components.ErrorBanner
import com.mibusrioja.app.ui.components.PrimaryButton
import com.mibusrioja.app.ui.components.ScreenHeader
import com.mibusrioja.app.ui.components.SecondaryButton
import com.mibusrioja.app.ui.components.StatusPill
import com.mibusrioja.app.ui.theme.Background
import com.mibusrioja.app.ui.theme.ErrorRed
import com.mibusrioja.app.ui.theme.GlassBorder
import com.mibusrioja.app.ui.theme.RiojaAmber
import com.mibusrioja.app.ui.theme.RiojaOrange
import com.mibusrioja.app.ui.theme.RiojaOrangeDark
import com.mibusrioja.app.ui.theme.SuccessGreen
import com.mibusrioja.app.ui.theme.SurfaceCard
import com.mibusrioja.app.ui.theme.SurfaceVariant
import com.mibusrioja.app.ui.theme.TextMuted
import com.mibusrioja.app.ui.theme.TextPrimary
import com.mibusrioja.app.ui.theme.TextSecondary
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun SaldoScreen(
    modifier: Modifier = Modifier,
    onOpenSalePoints: () -> Unit = {},
    viewModel: SaldoViewModel = hiltViewModel()
) {
    val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle()
    val captchaCode by viewModel.captchaCode.collectAsStateWithLifecycle()
    val captchaImage by viewModel.captchaImage.collectAsStateWithLifecycle()
    val isLoadingCaptcha by viewModel.isLoadingCaptcha.collectAsStateWithLifecycle()
    val saldo by viewModel.saldo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saveCard by viewModel.saveCard.collectAsStateWithLifecycle()
    val showVerification by viewModel.showVerification.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = Dimens.screenPadding,
                vertical = Dimens.xl
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            item {
                ScreenHeader(
                    title = "Mi saldo",
                    subtitle = "Consultá el saldo de tu tarjeta Red Bus"
                )
            }

            item {
                SaldoCardIllustration(
                    cardNumber = cardNumber,
                    saldo = saldo
                )
            }

            item {
                SaldoInputCard(
                    cardNumber = cardNumber,
                    saveCard = saveCard,
                    isLoading = isLoading,
                    onCardNumberChange = { viewModel.updateCardNumber(it) },
                    onSaveCardChange = { viewModel.setSaveCard(it) },
                    onConsultarClick = {
                        keyboardController?.hide()
                        viewModel.requestVerification()
                    }
                )
            }

            if (error != null && !showVerification) {
                item {
                    ErrorBanner(
                        message = error.orEmpty(),
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            saldo?.let { s ->
                item {
                    SaldoResultSection(saldo = s)
                }
            }

            if (saldo == null && !isLoading && error == null) {
                item {
                    SaldoEmptyTips()
                }
            }

            item {
                SecondaryButton(
                    text = "Puntos de recarga cercanos",
                    onClick = onOpenSalePoints,
                    icon = Icons.Rounded.Storefront
                )
            }
        }

        if (showVerification) {
            VerificationSheet(
                captchaImage = captchaImage,
                isLoadingCaptcha = isLoadingCaptcha,
                captchaCode = captchaCode,
                isLoading = isLoading,
                error = error,
                onCaptchaChange = { viewModel.updateCaptcha(it) },
                onRefreshCaptcha = { viewModel.loadCaptcha() },
                onConfirm = { viewModel.checkBalance() },
                onDismiss = { viewModel.dismissVerification() }
            )
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
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(Dimens.radiusLg))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        RiojaOrangeDark,
                        RiojaOrange,
                        SurfaceCard
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
                shape = RoundedCornerShape(Dimens.radiusLg)
            )
            .padding(Dimens.xl)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsBus,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Mi Bus Rioja - Red Bus",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.Contactless,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Icon(
                imageVector = Icons.Rounded.SimCard,
                contentDescription = null,
                tint = RiojaAmber,
                modifier = Modifier.size(36.dp)
            )

            Column {
                val formattedNumber = if (cardNumber.isNotBlank()) {
                    formatCardNumberDisplay(cardNumber)
                } else {
                    "•••• •••• •••• ••••"
                }

                Text(
                    text = formattedNumber,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(Dimens.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = saldo?.tipo?.uppercase(Locale.ROOT) ?: "TARJETA INTELIGENTE",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    )

                    Text(
                        text = "RED BUS",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SaldoInputCard(
    cardNumber: String,
    saveCard: Boolean,
    isLoading: Boolean,
    onCardNumberChange: (String) -> Unit,
    onSaveCardChange: (Boolean) -> Unit,
    onConsultarClick: () -> Unit
) {
    AppCard {
        AppTextField(
            value = cardNumber,
            onValueChange = onCardNumberChange,
            label = "Número de tarjeta",
            placeholder = "Ingresá el número impreso en tu tarjeta",
            leadingIcon = Icons.Outlined.CreditCard,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onConsultarClick() })
        )

        Spacer(modifier = Modifier.height(Dimens.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Guardar tarjeta para futuras consultas",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = saveCard,
                onCheckedChange = onSaveCardChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RiojaOrange,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(Dimens.md))

        PrimaryButton(
            text = "Consultar saldo",
            onClick = onConsultarClick,
            loading = isLoading
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerificationSheet(
    captchaImage: ByteArray?,
    isLoadingCaptcha: Boolean,
    captchaCode: String,
    isLoading: Boolean,
    error: String?,
    onCaptchaChange: (String) -> Unit,
    onRefreshCaptcha: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Verificación de seguridad",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(Dimens.xs))
        Text(
            text = "Red Bus pide confirmar el código de la imagen para consultar el saldo.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(Dimens.lg))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            val bitmap = remember(captchaImage) {
                captchaImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }
            Box(
                modifier = Modifier
                    .width(192.dp)
                    .height(67.dp)
                    .clip(RoundedCornerShape(Dimens.radiusSm))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    bitmap != null -> Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Código de seguridad"
                    )
                    isLoadingCaptcha -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = RiojaOrange,
                        strokeWidth = 2.dp
                    )
                    else -> Text(
                        text = "Sin imagen",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            IconButton(onClick = onRefreshCaptcha) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Actualizar imagen",
                    tint = RiojaOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.lg))

        AppTextField(
            value = captchaCode,
            onValueChange = onCaptchaChange,
            label = "Código de la imagen",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm() })
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(Dimens.md))
            ErrorBanner(message = error)
        }

        Spacer(modifier = Modifier.height(Dimens.lg))

        PrimaryButton(
            text = "Confirmar",
            onClick = onConfirm,
            loading = isLoading
        )
    }
}

@Composable
fun SaldoResultSection(saldo: Saldo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.md)
    ) {
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tarjeta N° ${saldo.nroTarjeta}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    StatusPill(
                        text = saldo.tipo.ifBlank { "Común" },
                        color = RiojaOrange
                    )
                    saldo.estado?.takeIf { it.isNotBlank() }?.let { estado ->
                        StatusPill(
                            text = estado,
                            color = if (estado.equals("Activa", ignoreCase = true)) SuccessGreen else ErrorRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.md))

            Text(
                text = saldo.etiqueta ?: "Saldo disponible",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = formatArgCurrency(saldo.saldo),
                color = RiojaOrange,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            saldo.fechaSaldo?.takeIf { it.isNotBlank() }?.let { fecha ->
                Text(
                    text = "Saldo al $fecha",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (saldo.saldo < 0) {
                Text(
                    text = "Saldo de emergencia utilizado",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusSm))
                .border(1.dp, GlassBorder, RoundedCornerShape(Dimens.radiusSm))
                .padding(Dimens.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = RiojaAmber,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Los movimientos detallados sólo están disponibles registrando la tarjeta en larioja.miredbus.com.ar",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun SaldoEmptyTips() {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(Dimens.sm))

        Text(
            text = "• Encontrás el código de 6 a 8 dígitos impreso en el frente o dorso de tu tarjeta plástica Mi Bus Rioja / Sirve.\n• La consulta es 100% gratuita y no requiere registrar una cuenta personal.\n• Podrás ver tus fondos actualizados y el historial de débitos y recargas.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 18.sp
        )
    }
}

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
