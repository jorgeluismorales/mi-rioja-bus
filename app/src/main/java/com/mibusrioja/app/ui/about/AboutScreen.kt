package com.mibusrioja.app.ui.about

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mibusrioja.app.BuildConfig
import com.mibusrioja.app.ui.components.AppCard
import com.mibusrioja.app.ui.components.Dimens
import com.mibusrioja.app.ui.components.ScreenHeader
import com.mibusrioja.app.ui.theme.Background
import com.mibusrioja.app.ui.theme.InfoBlue
import com.mibusrioja.app.ui.theme.RiojaOrange
import com.mibusrioja.app.ui.theme.TextMuted
import com.mibusrioja.app.ui.theme.TextPrimary
import com.mibusrioja.app.ui.theme.TextSecondary
import com.mibusrioja.app.ui.theme.WarningYellow

/**
 * Pantalla "Acerca de": aclara que la app es un proyecto personal independiente,
 * sin relación con Red Bus ni con Rioja Bus.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = Dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                    ScreenHeader(
                        title = "Acerca de la app",
                        subtitle = "Mi Bus Rioja · versión ${BuildConfig.VERSION_NAME}"
                    )
                }
            }

            item {
                AboutCard(
                    icon = Icons.Outlined.Info,
                    iconTint = WarningYellow,
                    title = "App no oficial",
                    body = "Mi Bus Rioja NO tiene ninguna relación con Red Bus ni con Rioja Bus, " +
                        "ni con la Municipalidad o el Gobierno de La Rioja. No es una app oficial " +
                        "ni está avalada por ninguna de esas entidades. Las marcas mencionadas " +
                        "pertenecen a sus respectivos dueños."
                )
            }

            item {
                AboutCard(
                    icon = Icons.Outlined.Lightbulb,
                    iconTint = RiojaOrange,
                    title = "¿Por qué existe?",
                    body = "La idea nace de una inquietud personal: la app original no tenía las " +
                        "opciones que yo necesitaba como usuario del colectivo, por ejemplo " +
                        "planificar un viaje de un punto a otro o programar alertas de proximidad " +
                        "para que me avise cuando el colectivo o mi parada están cerca. " +
                        "Así que decidí hacer mi propia versión con esas funciones."
                )
            }

            item {
                AboutCard(
                    icon = Icons.Outlined.Public,
                    iconTint = InfoBlue,
                    title = "De dónde salen los datos",
                    body = "Los recorridos, posiciones de colectivos, tarifas, saldo y puntos de " +
                        "venta se consultan en tiempo real desde el servicio público de " +
                        "larioja.miredbus.com.ar. Los mapas son de OpenStreetMap y la búsqueda " +
                        "de lugares usa Nominatim. La app no cobra nada, no tiene publicidad y " +
                        "no envía tus datos a ningún servidor propio: la ubicación, las alertas " +
                        "y el número de tarjeta guardado quedan sólo en tu teléfono."
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsBus,
                            contentDescription = null,
                            tint = RiojaOrange,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(Dimens.sm))
                        Text(
                            text = "Hecho en La Rioja, Argentina",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    body: String
) {
    AppCard {
        Column(modifier = Modifier.padding(Dimens.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.sm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(Dimens.sm))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
