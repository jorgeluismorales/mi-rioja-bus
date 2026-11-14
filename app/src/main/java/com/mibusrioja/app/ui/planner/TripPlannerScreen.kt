package com.mibusrioja.app.ui.planner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mibusrioja.app.domain.model.FrequentDestination
import com.mibusrioja.app.domain.model.PlaceSuggestion
import com.mibusrioja.app.domain.model.TripPlan
import com.mibusrioja.app.domain.model.TripStep
import com.mibusrioja.app.domain.model.TripStepType
import com.mibusrioja.app.ui.map.createPointMarkerDrawable
import com.mibusrioja.app.ui.map.createStopMarkerDrawable
import com.mibusrioja.app.ui.map.createTileSource
import androidx.compose.material.icons.outlined.NotificationsActive
import com.mibusrioja.app.ui.theme.SuccessGreen
import com.mibusrioja.app.ui.components.AppBottomSheet
import com.mibusrioja.app.ui.components.AppCard
import com.mibusrioja.app.ui.components.AppTextField
import com.mibusrioja.app.ui.components.Dimens
import com.mibusrioja.app.ui.components.PrimaryButton
import com.mibusrioja.app.ui.components.ScreenHeader
import com.mibusrioja.app.ui.components.SecondaryButton
import com.mibusrioja.app.ui.components.SectionTitle
import com.mibusrioja.app.ui.components.StatusPill
import com.mibusrioja.app.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun TripPlannerScreen(
    viewModel: TripViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tripPlan by viewModel.tripPlan.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val originLocation by viewModel.originLocation.collectAsStateWithLifecycle()
    val destDescription by viewModel.destDescription.collectAsStateWithLifecycle()
    val destLocation by viewModel.destLocation.collectAsStateWithLifecycle()
    val alertsAutoCreated by viewModel.alertsAutoCreated.collectAsStateWithLifecycle()
    val isActivatingAlerts by viewModel.isActivatingAlerts.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val tileUrl by viewModel.tileUrl.collectAsStateWithLifecycle()
    val frequentDestinations by viewModel.frequentDestinations.collectAsStateWithLifecycle()
    val originLabel by viewModel.originLabel.collectAsStateWithLifecycle()
    val originIsGps by viewModel.originIsGps.collectAsStateWithLifecycle()
    val originSuggestions by viewModel.originSuggestions.collectAsStateWithLifecycle()
    var showOriginSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Manejo de permisos de GPS
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.setOriginFromGps(location.latitude, location.longitude)
                } else {
                    // Fallback para testing: Centro de La Rioja
                    viewModel.setOriginFromGps(-29.4134003, -66.8560292)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.setOriginFromGps(location.latitude, location.longitude)
                } else {
                    viewModel.setOriginFromGps(-29.4134003, -66.8560292)
                }
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Snackbar para alertas automáticas
    LaunchedEffect(alertsAutoCreated) {
        if (alertsAutoCreated > 0) {
            snackbarHostState.showSnackbar(
                message = "Se activaron $alertsAutoCreated ${if (alertsAutoCreated == 1) "alerta" else "alertas"} de proximidad para este viaje"
            )
        }
    }

    // Snackbar para errores
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título de la pantalla
                item {
                    ScreenHeader(
                        title = "Planificador de Viaje",
                        subtitle = "Encontrá el mejor recorrido en colectivo"
                    )
                }

                // Origen
                item {
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(RiojaOrange.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MyLocation,
                                    contentDescription = null,
                                    tint = RiojaOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = originLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (originIsGps) "Cerca de vos" else "Punto de partida elegido",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = { showOriginSheet = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Cambiar punto de partida",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Destino
                item {
                    AppCard {
                        AppTextField(
                            value = destDescription,
                            onValueChange = { viewModel.onDestinationTextChanged(it) },
                            placeholder = "¿A dónde querés ir en La Rioja?",
                            leadingIcon = Icons.Outlined.Search,
                            trailingIcon = if (destDescription.isNotBlank()) {
                                {
                                    IconButton(onClick = { viewModel.onDestinationTextChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else null
                        )

                        if (suggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Dimens.sm))
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm - Dimens.xs)) {
                                suggestions.take(6).forEach { suggestion ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectSuggestion(suggestion) },
                                        shape = RoundedCornerShape(Dimens.radiusSm),
                                        color = SurfaceVariant,
                                        border = BorderStroke(1.dp, DividerColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm + 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = RiojaOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(Dimens.sm))
                                            Column {
                                                Text(
                                                    text = suggestion.nombre,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = suggestion.detalle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(Dimens.md))

                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            RIOJA_DESTINATIONS.take(2).forEach { preset ->
                                val isSelected = destLocation?.first == preset.lat && destLocation?.second == preset.lng
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setDestination(preset.title, preset.lat, preset.lng, preset.subtitle)
                                    },
                                    label = { Text(preset.title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RiojaOrange,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceVariant,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) RiojaOrange else DividerColor,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Dimens.lg))

                        SectionTitle(text = "Tus destinos frecuentes")

                        if (frequentDestinations.isEmpty()) {
                            Text(
                                text = "Acá van a aparecer los lugares a los que viajás seguido.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                                frequentDestinations.forEach { destination ->
                                    FrequentDestinationRow(
                                        destination = destination,
                                        onClick = { viewModel.selectFrequent(destination) },
                                        onRemove = { viewModel.removeFrequent(destination.key) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(Dimens.lg))

                        PrimaryButton(
                            text = "Buscar recorrido",
                            onClick = { viewModel.planTrip() },
                            enabled = destLocation != null,
                            loading = isLoading,
                            icon = Icons.Default.AltRoute
                        )
                    }
                }

                // ── RESULTADOS o ESTADO VACÍO ──────────────────────────────────────────
                if (tripPlan != null) {
                    item {
                        TripResultsHeader(
                            tripPlan = tripPlan!!,
                            onClear = { viewModel.clearPlan() }
                        )
                    }

                    item {
                        TripPlanMapView(
                            tripPlan = tripPlan!!,
                            tileUrl = tileUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    item {
                        if (alertsAutoCreated > 0) {
                            StatusPill(
                                text = "Alertas de proximidad activadas para este viaje",
                                color = SuccessGreen
                            )
                        } else {
                            SecondaryButton(
                                text = "Activar alertas de este viaje",
                                onClick = { viewModel.activateTripAlerts() },
                                enabled = !isActivatingAlerts,
                                icon = Icons.Outlined.NotificationsActive
                            )
                        }
                    }

                    itemsIndexed(tripPlan!!.pasos) { index, step ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 30 * (index + 1) })
                        ) {
                            TripStepCard(
                                step = step,
                                isLast = index == tripPlan!!.pasos.lastIndex
                            )
                        }
                    }
                } else if (!isLoading) {
                    item {
                        TripPlannerEmptyState(
                            onSelectPreset = { preset ->
                                viewModel.setDestination(preset.title, preset.lat, preset.lng)
                                viewModel.planTrip()
                            }
                        )
                    }
                }
            }

            // Overlay de carga con bus animado
            if (isLoading) {
                TripLoadingOverlay()
            }

            if (showOriginSheet) {
                OriginSheet(
                    suggestions = originSuggestions,
                    onUseGps = {
                        if (hasLocationPermission) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                loc?.let { viewModel.setOriginFromGps(it.latitude, it.longitude) }
                                    ?: viewModel.setOriginFromGps(-29.4134003, -66.8560292)
                            }
                        } else {
                            requestPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        showOriginSheet = false
                    },
                    onQueryChanged = { viewModel.onOriginQueryChanged(it) },
                    onSelectSuggestion = { suggestion ->
                        viewModel.selectOriginSuggestion(suggestion)
                        showOriginSheet = false
                    },
                    onDismiss = { showOriginSheet = false }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequentDestinationRow(
    destination: FrequentDestination,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusSm),
        color = SurfaceVariant,
        border = BorderStroke(1.dp, DividerColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = RiojaOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = destination.detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                text = "${destination.vecesUsado} ${if (destination.vecesUsado == 1) "viaje" else "viajes"}",
                color = RiojaOrange
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitar de frecuentes",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OriginSheet(
    suggestions: List<PlaceSuggestion>,
    onUseGps: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSelectSuggestion: (PlaceSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Punto de partida",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(Dimens.lg))

        SecondaryButton(
            text = "Usar mi ubicación actual",
            onClick = onUseGps,
            icon = Icons.Outlined.MyLocation
        )

        Spacer(modifier = Modifier.height(Dimens.md))

        AppTextField(
            value = query,
            onValueChange = {
                query = it
                onQueryChanged(it)
            },
            placeholder = "Buscá una dirección o lugar",
            leadingIcon = Icons.Outlined.Search
        )

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm - Dimens.xs)) {
                suggestions.take(6).forEach { suggestion ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSuggestion(suggestion) },
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        color = SurfaceVariant,
                        border = BorderStroke(1.dp, DividerColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm + 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = RiojaOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            Column {
                                Text(
                                    text = suggestion.nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = suggestion.detalle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun TripResultsHeader(
    tripPlan: TripPlan,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Recorrido sugerido",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${tripPlan.pasos.size} pasos en tu itinerario · ≈ ${tripPlan.duracionEstimadaMin} min",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        TextButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = RiojaOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Nueva búsqueda",
                style = MaterialTheme.typography.labelMedium,
                color = RiojaOrange,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TripStepCard(
    step: TripStep,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Indicador lateral de línea temporal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (step.tipo) {
                            TripStepType.CAMINAR -> RiojaAmber.copy(alpha = 0.2f)
                            TripStepType.TOMAR_COLECTIVO -> RiojaOrange.copy(alpha = 0.2f)
                            TripStepType.TRANSBORDO -> InfoBlue.copy(alpha = 0.2f)
                            TripStepType.LLEGAR -> SuccessGreen.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (step.tipo) {
                        TripStepType.CAMINAR -> Icons.Default.DirectionsWalk
                        TripStepType.TOMAR_COLECTIVO -> Icons.Default.DirectionsBus
                        TripStepType.TRANSBORDO -> Icons.Default.SwapHoriz
                        TripStepType.LLEGAR -> Icons.Default.Flag
                    },
                    contentDescription = null,
                    tint = when (step.tipo) {
                        TripStepType.CAMINAR -> RiojaAmber
                        TripStepType.TOMAR_COLECTIVO -> RiojaOrange
                        TripStepType.TRANSBORDO -> InfoBlue
                        TripStepType.LLEGAR -> SuccessGreen
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(DividerColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Tarjeta con contenido del paso
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                when (step.tipo) {
                    TripStepType.CAMINAR -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Caminar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RiojaAmber
                            )
                            if (step.distanciaMetros > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RiojaAmber.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${step.distanciaMetros}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RiojaAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = step.instruccion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }

                    TripStepType.TOMAR_COLECTIVO -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RiojaOrange,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = step.linea?.codLinea ?: "LÍNEA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = step.linea?.descripcion ?: "Colectivo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = step.instruccion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        if (step.paradaOrigen != null || step.paradaDestino != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    if (step.paradaOrigen?.nombreParada != null) {
                                        Text(
                                            text = "Subida: ${step.paradaOrigen.nombreParada}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    if (step.paradaDestino?.nombreParada != null) {
                                        Text(
                                            text = "Bajada: ${step.paradaDestino.nombreParada}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TripStepType.TRANSBORDO -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transbordo de línea",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = InfoBlue
                            )

                            // Badge: Alerta 100m configurada
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RiojaOrange.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, RiojaOrange.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = RiojaOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "⚡ Alerta 100m configurada",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RiojaOrange
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = step.instruccion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }

                    TripStepType.LLEGAR -> {
                        Text(
                            text = "¡Llegaste!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = step.instruccion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripPlannerEmptyState(
    onSelectPreset: (DestinationPreset) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(RiojaOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = RiojaOrange,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¿Cómo funciona el planificador?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "1. Detectamos automáticamente tu ubicación actual por GPS.\n" +
                        "2. Seleccionás o buscás tu destino en la ciudad.\n" +
                        "3. Calculamos la ruta más rápida, directa o con transbordo.\n" +
                        "4. Configuramos una alerta automática a 100m de tu parada de combinación.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Destinos populares recomendados:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = RiojaOrangeLight
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RIOJA_DESTINATIONS.take(3).forEach { preset ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPreset(preset) },
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariant,
                        border = BorderStroke(1.dp, DividerColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = RiojaOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripLoadingOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "busMovement")
    val translationX by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "busOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = RiojaOrange,
                        modifier = Modifier
                            .size(44.dp)
                            .offset(x = translationX.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Buscando mejor ruta…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Calculando paradas y posibles transbordos",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    color = RiojaOrange,
                    trackColor = SurfaceVariant,
                    modifier = Modifier
                        .width(140.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * Mapa embebido del itinerario: polylines de los tramos en colectivo,
 * marcadores de paradas de subida/bajada/transbordo y puntos de origen/destino.
 */
@Composable
fun TripPlanMapView(
    tripPlan: TripPlan,
    tileUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val lifecycleMap = mapViewRef
    DisposableEffect(lifecycleOwner, lifecycleMap) {
        val map = lifecycleMap
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map?.onResume()
                Lifecycle.Event.ON_PAUSE -> map?.onPause()
                Lifecycle.Event.ON_DESTROY -> map?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            map?.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(createTileSource(tileUrl))
                setMultiTouchControls(true)
                setDestroyMode(false)
                isTilesScaledToDpi = true
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                )
                controller.setZoom(13.0)
                controller.setCenter(GeoPoint(-29.4134003, -66.8560292))
                mapViewRef = this
            }
        },
        update = { mapView ->
            val target = createTileSource(tileUrl)
            if (mapView.tileProvider.tileSource.name() != target.name()) {
                mapView.setTileSource(target)
            }

            mapView.overlays.clear()

            val allPoints = mutableListOf<GeoPoint>()

            // Polylines de los tramos en colectivo (naranja la primera, azul la segunda)
            val busSteps = tripPlan.pasos.filter { it.tipo == TripStepType.TOMAR_COLECTIVO }
            busSteps.forEachIndexed { index, step ->
                val points = step.recorrido.map { GeoPoint(it.lat, it.lng) }
                if (points.size >= 2) {
                    allPoints.addAll(points)
                    val polyline = Polyline(mapView).apply {
                        outlinePaint.color = (if (index == 0) RiojaOrange else InfoBlue).toArgb()
                        outlinePaint.strokeWidth = 8f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        setPoints(points)
                    }
                    mapView.overlays.add(0, polyline)
                }
            }

            // Marcadores de paradas de subida, bajada y transbordo
            val stopIcon = createStopMarkerDrawable(context)
            tripPlan.pasos.forEach { step ->
                listOfNotNull(step.paradaOrigen, step.paradaDestino).forEach { parada ->
                    val point = GeoPoint(parada.lat, parada.lng)
                    allPoints.add(point)
                    val marker = Marker(mapView).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = stopIcon
                        title = parada.nombreParada ?: "Parada ${parada.codParada ?: ""}"
                    }
                    mapView.overlays.add(marker)
                }
            }

            // Marcador de origen (azul) y destino (naranja)
            tripPlan.origen?.let { (lat, lng) ->
                val point = GeoPoint(lat, lng)
                allPoints.add(point)
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createPointMarkerDrawable(context, MapUserColor.toArgb())
                        title = "Origen"
                    }
                )
            }
            tripPlan.destino?.let { (lat, lng) ->
                val point = GeoPoint(lat, lng)
                allPoints.add(point)
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createPointMarkerDrawable(context, RiojaOrange.toArgb())
                        title = "Destino"
                    }
                )
            }

            if (allPoints.size >= 2) {
                try {
                    mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(allPoints), false, 100)
                } catch (_: Exception) {
                }
            }
            mapView.invalidate()
        }
    )
}

