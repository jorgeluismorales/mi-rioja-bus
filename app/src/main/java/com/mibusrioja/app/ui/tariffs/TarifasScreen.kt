package com.mibusrioja.app.ui.tariffs

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessible
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mibusrioja.app.domain.usecase.TarifaDisplay
import com.mibusrioja.app.domain.usecase.TarifaKind
import com.mibusrioja.app.ui.components.AppCard
import com.mibusrioja.app.ui.components.Dimens
import com.mibusrioja.app.ui.components.EmptyState
import com.mibusrioja.app.ui.components.ErrorBanner
import com.mibusrioja.app.ui.components.AboutIconButton
import com.mibusrioja.app.ui.components.ScreenHeader
import com.mibusrioja.app.ui.components.StatusPill
import com.mibusrioja.app.ui.theme.Background
import com.mibusrioja.app.ui.theme.RiojaOrange
import com.mibusrioja.app.ui.theme.SuccessGreen
import com.mibusrioja.app.ui.theme.TextMuted
import com.mibusrioja.app.ui.theme.TextPrimary
import com.mibusrioja.app.ui.theme.TextSecondary
import com.mibusrioja.app.ui.theme.WarningYellow

@Composable
fun TarifasScreen(
    modifier: Modifier = Modifier,
    onOpenAbout: () -> Unit = {},
    viewModel: TarifasViewModel = hiltViewModel()
) {
    val displays by viewModel.tarifaDisplays.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

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
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            item {
                ScreenHeader(
                    title = "Tarifas Vigentes",
                    subtitle = "Precios oficiales de Red Bus La Rioja",
                    trailing = { AboutIconButton(onClick = onOpenAbout) }
                )
            }

            if (error != null) {
                item {
                    ErrorBanner(
                        message = error.orEmpty(),
                        onDismiss = { viewModel.loadTarifas() },
                        onRetry = { viewModel.loadTarifas() }
                    )
                }
            }

            when {
                isLoading && displays.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.xxl),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = RiojaOrange)
                        }
                    }
                }
                displays.isNotEmpty() -> {
                    itemsIndexed(displays, key = { i, _ -> i }) { _, display ->
                        TarifaCard(display = display)
                    }

                    item {
                        TarifasFooterNote()
                    }
                }
                !isLoading && error == null -> {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Sell,
                            title = "Sin tarifas disponibles",
                            message = "No pudimos cargar las tarifas vigentes en este momento.",
                            actionLabel = "Reintentar",
                            onAction = { viewModel.refresh() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarifaCard(display: TarifaDisplay) {
    val (icon, iconTint) = when (display.kind) {
        TarifaKind.ESTANDAR -> Icons.Outlined.ConfirmationNumber to RiojaOrange
        TarifaKind.DESCUENTO -> Icons.Outlined.Percent to WarningYellow
        TarifaKind.GRATIS -> Icons.Outlined.Accessible to SuccessGreen
    }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(end = Dimens.md),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = display.categoria,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (display.etiqueta != null) {
                    Spacer(modifier = Modifier.height(Dimens.xs))
                    StatusPill(
                        text = display.etiqueta,
                        color = if (display.kind == TarifaKind.DESCUENTO) WarningYellow else SuccessGreen
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = display.precioFinal,
                    style = if (display.kind == TarifaKind.GRATIS) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = RiojaOrange
                )
                if (display.precioBase != null) {
                    Text(
                        text = display.precioBase,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }
    }
}

@Composable
private fun TarifasFooterNote() {
    Text(
        text = "Valores actualizados según la información publicada por Red Bus. Los beneficios aplican presentando la credencial correspondiente.",
        color = TextSecondary,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.lg)
    )
}
