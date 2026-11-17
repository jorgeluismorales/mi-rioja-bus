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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material3.Icon
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
import com.mibusrioja.app.ui.theme.RiojaOrange
import com.mibusrioja.app.ui.theme.TextMuted
import com.mibusrioja.app.ui.theme.TextPrimary
import com.mibusrioja.app.ui.theme.TextSecondary
import com.mibusrioja.app.ui.theme.WarningYellow

/**
 * Pantalla "Acerca de": aclara que la app es un proyecto independiente,
 * sin relación con Red Bus ni con Rioja Bus, y explica su origen.
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = Background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = Dimens.xl),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            item {
                ScreenHeader(
                    title = "Acerca de la app",
                    subtitle = "Mi Bus Rioja · versión ${BuildConfig.VERSION_NAME}"
                )
            }

            item {
                AboutCard(
                    icon = Icons.Outlined.Info,
                    iconTint = WarningYellow,
                    title = "Aplicación independiente",
                    body = "Mi Bus Rioja es un proyecto independiente y sin fines de lucro. " +
                        "No mantiene relación, vínculo ni afiliación alguna con Red Bus, Rioja Bus " +
                        "ni con los organismos públicos de la provincia de La Rioja, y no constituye " +
                        "una aplicación oficial ni avalada por dichas entidades. Las marcas y nombres " +
                        "mencionados pertenecen a sus respectivos titulares."
                )
            }

            item {
                AboutCard(
                    icon = Icons.Outlined.Lightbulb,
                    iconTint = RiojaOrange,
                    title = "Origen del proyecto",
                    body = "Esta aplicación surge de una inquietud personal como usuario del " +
                        "transporte público. La aplicación oficial no ofrecía funciones que " +
                        "consideraba esenciales para el uso diario, como la planificación de viajes " +
                        "entre dos puntos de la ciudad o la programación de alertas de proximidad. " +
                        "Mi Bus Rioja nace con el objetivo de cubrir esas necesidades y ofrecer una " +
                        "experiencia más completa a quienes se mueven en colectivo por La Rioja."
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
