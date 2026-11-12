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
import com.mibusrioja.app.domain.model.TripPlan
import com.mibusrioja.app.domain.model.TripStep
import com.mibusrioja.app.domain.model.TripStepType
import com.mibusrioja.app.ui.theme.*
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
                message = "⚡ Se configuraron $alertsAutoCreated alertas de transbordo automáticamente"
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
                    Column {
                        Text(
                            text = "Planificador de Viaje",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Combinaciones inteligentes y alertas de transbordo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Sección superior: Tarjeta con campos de Origen y Destino
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── ORIGEN: GPS ──────────────────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Punto azul pulsante GPS
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MapUserColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MapUserColor)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tu ubicación (GPS)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MapUserColor
                                    )
                                    val coordsText = if (originLocation != null) {
                                        "Lat: ${String.format(Locale.US, "%.4f", originLocation!!.first)}, Lng: ${String.format(Locale.US, "%.4f", originLocation!!.second)}"
                                    } else {
                                        "Buscando satélites GPS..."
                                    }
                                    Text(
                                        text = coordsText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (hasLocationPermission) {
                                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                                loc?.let { viewModel.setOriginFromGps(it.latitude, it.longitude) }
                                                    ?: viewModel.setOriginFromGps(-29.4134003, -66.8560292)
                                            }
                                        } else {
                                            requestPermissionLauncher.launch(
                                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Actualizar GPS",
                                        tint = MapUserColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Divider(color = DividerColor.copy(alpha = 0.5f), thickness = 1.dp)

                            // ── DESTINO ──────────────────────────────────────────────
                            OutlinedTextField(
                                value = destDescription,
                                onValueChange = { viewModel.setDestinationDescription(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "¿A dónde querés ir en La Rioja?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = RiojaOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (destDescription.isNotBlank()) {
                                        IconButton(onClick = { viewModel.setDestinationDescription("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Limpiar",
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RiojaOrange,
                                    unfocusedBorderColor = DividerColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark
                                )
                            )

                            // Chips de destinos frecuentes en La Rioja
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(RIOJA_DESTINATIONS) { preset ->
                                    val isSelected = destLocation?.first == preset.lat && destLocation?.second == preset.lng
                                    Surface(
                                        modifier = Modifier.clickable {
                                            viewModel.setDestination(preset.title, preset.lat, preset.lng)
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) RiojaOrange.copy(alpha = 0.2f) else SurfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) RiojaOrange else DividerColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = if (isSelected) RiojaOrange else TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = preset.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) RiojaOrange else TextSecondary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Botón Planificar (RiojaOrange, ancho completo)
                            Button(
                                onClick = { viewModel.planTrip() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RiojaOrange),
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AltRoute,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Planificar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
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
                text = "${tripPlan.pasos.size} pasos en tu itinerario",
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
