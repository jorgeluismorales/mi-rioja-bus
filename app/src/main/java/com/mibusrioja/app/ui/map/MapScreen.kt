package com.mibusrioja.app.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.mibusrioja.app.domain.model.AppConfig
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.BusPosition
import com.mibusrioja.app.domain.model.LineGroup
import com.mibusrioja.app.domain.model.RouteNode
import com.mibusrioja.app.domain.model.TiempoEstimado
import com.mibusrioja.app.ui.theme.Background
import com.mibusrioja.app.ui.theme.DividerColor
import com.mibusrioja.app.ui.theme.RiojaOrange
import com.mibusrioja.app.ui.theme.RiojaOrangeDark
import com.mibusrioja.app.ui.theme.RiojaOrangeLight
import com.mibusrioja.app.ui.theme.SuccessGreen
import com.mibusrioja.app.ui.theme.SurfaceCard
import com.mibusrioja.app.ui.theme.SurfaceDark
import com.mibusrioja.app.ui.theme.TextMuted
import com.mibusrioja.app.ui.theme.TextPrimary
import com.mibusrioja.app.ui.theme.TextSecondary
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val DEFAULT_LATITUDE = -29.4134003
private const val DEFAULT_LONGITUDE = -66.8560292
private const val DEFAULT_ZOOM = 13.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: BusMapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ViewModel StateFlows
    val lineGroups by viewModel.lineGroups.collectAsStateWithLifecycle()
    val selectedLine by viewModel.selectedLine.collectAsStateWithLifecycle()
    val routeNodes by viewModel.routeNodes.collectAsStateWithLifecycle()
    val busPositions by viewModel.busPositions.collectAsStateWithLifecycle()
    val selectedStop by viewModel.selectedStop.collectAsStateWithLifecycle()
    val stopArrivalTimes by viewModel.stopArrivalTimes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingArrivals by viewModel.isLoadingArrivals.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()

    // MapView & Overlays references
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var locationOverlayInstance by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    val polylineRef = remember { mutableStateOf<Polyline?>(null) }
    val stopMarkers = remember { mutableListOf<Marker>() }
    val busMarkersMap = remember { mutableMapOf<String, Marker>() }
    val lastRouteLineCode = remember { mutableStateOf<String?>(null) }
    val lastRouteNodeCount = remember { mutableStateOf(0) }

    // Custom Marker Drawables
    val stopIconDrawable = remember(context) { createStopMarkerDrawable(context) }
    val busIconDrawable = remember(context) { createBusMarkerDrawable(context) }

    // GPS Location Client
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            locationOverlayInstance?.enableMyLocation()
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        mapViewInstance?.controller?.animateTo(
                            GeoPoint(loc.latitude, loc.longitude),
                            16.0,
                            800L
                        )
                    }
                }
            } catch (_: SecurityException) { }
        }
    }

    // MapView Lifecycle management
    DisposableEffect(lifecycleOwner, mapViewInstance) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewInstance?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewInstance?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewInstance?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationOverlayInstance?.disableMyLocation()
            mapViewInstance?.onDetach()
        }
    }

    // Error handling snackbar
    LaunchedEffect(error) {
        error?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Cerrar",
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 1. MapView en pantalla completa ──────────────────────────────
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(createTileSource(appConfig))
                        setMultiTouchControls(true)
                        isTilesScaledToDpi = true
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(DEFAULT_ZOOM)
                        controller.setCenter(
                            GeoPoint(
                                appConfig?.latCenter ?: DEFAULT_LATITUDE,
                                appConfig?.lngCenter ?: DEFAULT_LONGITUDE
                            )
                        )

                        // Configurar overlay de ubicación GPS
                        val provider = GpsMyLocationProvider(ctx)
                        val locOverlay = MyLocationNewOverlay(provider, this).apply {
                            setDrawAccuracyEnabled(true)
                            if (hasLocationPermission) {
                                enableMyLocation()
                            }
                        }
                        overlays.add(locOverlay)
                        locationOverlayInstance = locOverlay
                        mapViewInstance = this
                    }
                },
                update = { mapView ->
                    // 1. Actualizar fuente de tiles si cargó la configuración
                    val targetTileSource = createTileSource(appConfig)
                    if (mapView.tileProvider.tileSource.name() != targetTileSource.name()) {
                        mapView.setTileSource(targetTileSource)
                    }

                    // 2. Actualizar recorrido (polyline y paradas) cuando cambian los nodos
                    val currentLineCode = selectedLine?.codLinea
                    val routeChanged = currentLineCode != lastRouteLineCode.value ||
                            routeNodes.size != lastRouteNodeCount.value

                    if (routeChanged) {
                        lastRouteLineCode.value = currentLineCode
                        lastRouteNodeCount.value = routeNodes.size

                        // Remover polyline anterior
                        polylineRef.value?.let { mapView.overlays.remove(it) }
                        polylineRef.value = null

                        // Remover paradas anteriores
                        stopMarkers.forEach { mapView.overlays.remove(it) }
                        stopMarkers.clear()

                        // Limpiar colectivos anteriores al cambiar de línea
                        busMarkersMap.values.forEach { mapView.overlays.remove(it) }
                        busMarkersMap.clear()

                        if (routeNodes.isNotEmpty()) {
                            val geoPoints = routeNodes.map { GeoPoint(it.lat, it.lng) }

                            // Dibujar Polyline en RiojaOrange
                            val polyline = Polyline(mapView).apply {
                                outlinePaint.color = RiojaOrange.toArgb()
                                outlinePaint.strokeWidth = 10f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                                setPoints(geoPoints)
                            }
                            polylineRef.value = polyline
                            mapView.overlays.add(0, polyline)

                            // Dibujar paradas
                            val paradas = routeNodes.filter { it.esParada }
                            paradas.forEach { stopNode ->
                                val stopMarker = Marker(mapView).apply {
                                    position = GeoPoint(stopNode.lat, stopNode.lng)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    icon = stopIconDrawable
                                    title = stopNode.nombreParada?.ifBlank { null }
                                        ?: "Parada #${stopNode.codParada ?: ""}"
                                    setOnMarkerClickListener { _, _ ->
                                        viewModel.selectStop(stopNode)
                                        true
                                    }
                                }
                                stopMarkers.add(stopMarker)
                                mapView.overlays.add(stopMarker)
                            }

                            // Ajustar zoom y encuadre del recorrido
                            if (geoPoints.size >= 2) {
                                try {
                                    val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                                    mapView.zoomToBoundingBox(boundingBox, true, 120)
                                } catch (_: Exception) {
                                    mapView.controller.animateTo(geoPoints.first(), 14.0, 600L)
                                }
                            }
                        }
                        mapView.invalidate()
                    }

                    // 3. Actualizar marcadores de buses en tiempo real (polling cada 15s)
                    val currentInternos = busPositions.map { it.interno }.toSet()

                    // Eliminar buses que ya no reportan posición
                    val iterator = busMarkersMap.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (entry.key !in currentInternos) {
                            mapView.overlays.remove(entry.value)
                            iterator.remove()
                        }
                    }

                    // Agregar o actualizar posición y rotación de buses
                    for (bus in busPositions) {
                        val targetGeoPoint = GeoPoint(bus.lat, bus.lng)
                        val existing = busMarkersMap[bus.interno]
                        if (existing != null) {
                            existing.position = targetGeoPoint
                            existing.rotation = bus.orientacion
                            existing.snippet = if (bus.tieneRampa) "Accesible con rampa" else "Sin rampa"
                        } else {
                            val newBusMarker = Marker(mapView).apply {
                                id = bus.interno
                                position = targetGeoPoint
                                rotation = bus.orientacion
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = busIconDrawable
                                title = "Interno ${bus.interno}"
                                snippet = if (bus.tieneRampa) "Accesible con rampa" else "Sin rampa"
                                setOnMarkerClickListener { m, _ ->
                                    m.showInfoWindow()
                                    true
                                }
                            }
                            busMarkersMap[bus.interno] = newBusMarker
                            mapView.overlays.add(newBusMarker)
                        }
                    }

                    mapView.invalidate()
                }
            )

            // ── 2. Sección superior: Chips de grupos y líneas ────────────────
            LineSelectionHeader(
                lineGroups = lineGroups,
                selectedLine = selectedLine,
                onLineSelected = { line -> viewModel.selectLine(line) },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // ── 3. Badge informativo de colectivos activos ────────────────────
            if (selectedLine != null && busPositions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SuccessGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${busPositions.size} ${if (busPositions.size == 1) "colectivo activo" else "colectivos activos"}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── 4. Estado vacío: 'Seleccioná una línea' ───────────────────────
            if (selectedLine == null && !isLoading) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, RiojaOrange.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(RiojaOrange.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = RiojaOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Seleccioná una línea para ver el recorrido",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Elegí una línea de colectivos arriba para ver su ruta y paradas.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // ── 5. FAB: 'Mi ubicación' ───────────────────────────────────────
            FloatingActionButton(
                onClick = {
                    if (hasLocationPermission) {
                        val myLoc = locationOverlayInstance?.myLocation
                        if (myLoc != null) {
                            mapViewInstance?.controller?.animateTo(myLoc, 16.0, 800L)
                        } else {
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        mapViewInstance?.controller?.animateTo(
                                            GeoPoint(loc.latitude, loc.longitude),
                                            16.0,
                                            800L
                                        )
                                    }
                                }
                            } catch (_: SecurityException) { }
                        }
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = RiojaOrange,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Mi ubicación"
                )
            }

            // ── 6. Overlay de carga ──────────────────────────────────────────
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, RiojaOrange.copy(alpha = 0.3f)),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = RiojaOrange,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = if (selectedLine != null) "Cargando recorrido..." else "Cargando datos...",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // ── 7. ModalBottomSheet de tiempos estimados para parada ───────────────
    if (selectedStop != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelectedStop() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
            tonalElevation = 8.dp,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = TextMuted)
            }
        ) {
            StopArrivalBottomSheetContent(
                stop = selectedStop!!,
                selectedLine = selectedLine,
                arrivalTimes = stopArrivalTimes,
                isLoading = isLoadingArrivals,
                onDismiss = { viewModel.clearSelectedStop() }
            )
        }
    }
}

/**
 * Cabecera superior con chips horizontales scrolleables para grupos y líneas de colectivos.
 */
@Composable
private fun LineSelectionHeader(
    lineGroups: List<LineGroup>,
    selectedLine: BusLine?,
    onLineSelected: (BusLine) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGroup by remember(lineGroups) {
        mutableStateOf(lineGroups.firstOrNull())
    }

    // Mantener sincronizado el grupo si la línea seleccionada pertenece a otro grupo
    LaunchedEffect(selectedLine, lineGroups) {
        if (selectedLine != null) {
            val matchingGroup = lineGroups.firstOrNull { group ->
                group.extractAllLines().any { it.codLinea == selectedLine.codLinea }
            }
            if (matchingGroup != null && matchingGroup != selectedGroup) {
                selectedGroup = matchingGroup
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Background.copy(alpha = 0.95f),
                        Background.copy(alpha = 0.85f),
                        Background.copy(alpha = 0.0f)
                    )
                )
            )
            .padding(top = 10.dp, bottom = 14.dp)
    ) {
        // Fila 1: Grupos de líneas (Troncales, Complementarias, Rurales, etc.)
        if (lineGroups.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(lineGroups, key = { it.nombre }) { group ->
                    val isGroupSelected = group == selectedGroup
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isGroupSelected) RiojaOrange else SurfaceCard,
                        border = if (isGroupSelected) null else BorderStroke(1.dp, DividerColor),
                        onClick = { selectedGroup = group },
                        modifier = Modifier.animateContentSize()
                    ) {
                        Text(
                            text = formatGroupName(group.nombre),
                            color = if (isGroupSelected) Color.White else TextSecondary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isGroupSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Fila 2: Líneas dentro del grupo seleccionado
        val linesToShow = remember(selectedGroup, lineGroups) {
            selectedGroup?.extractAllLines() ?: lineGroups.flatMap { it.extractAllLines() }
        }

        if (linesToShow.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(linesToShow, key = { it.codLinea }) { line ->
                    val isLineSelected = line.codLinea == selectedLine?.codLinea
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLineSelected) RiojaOrange.copy(alpha = 0.22f) else SurfaceDark.copy(alpha = 0.85f),
                        border = BorderStroke(
                            width = if (isLineSelected) 1.5.dp else 1.dp,
                            color = if (isLineSelected) RiojaOrange else DividerColor
                        ),
                        onClick = { onLineSelected(line) },
                        modifier = Modifier.animateContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = if (isLineSelected) RiojaOrange else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = line.descripcion.ifBlank { "Línea ${line.codLinea}" },
                                color = if (isLineSelected) TextPrimary else TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isLineSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Contenido del ModalBottomSheet al tocar una parada de colectivo.
 */
@Composable
private fun StopArrivalBottomSheetContent(
    stop: RouteNode,
    selectedLine: BusLine?,
    arrivalTimes: List<TiempoEstimado>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Cabecera con nombre de la parada y código
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(RiojaOrange.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = RiojaOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.nombreParada?.ifBlank { "Parada" }
                        ?: "Parada #${stop.codParada ?: ""}",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (!stop.codParada.isNullOrBlank()) {
                            append("Parada N° ${stop.codParada}")
                        }
                        if (selectedLine != null) {
                            if (isNotEmpty()) append(" • ")
                            append(selectedLine.descripcion)
                        }
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(16.dp))

        // Sección de tiempos estimados
        Text(
            text = "Próximos arribos",
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = RiojaOrange,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Consultando cuándo viene...",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            arrivalTimes.isEmpty() -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sin arribos próximos confirmados",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Los colectivos de esta línea aún no registran arribo próximo hacia esta parada.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    arrivalTimes.forEach { tiempo ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, DividerColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(RiojaOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = RiojaOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (!tiempo.interno.isNullOrBlank()) "Interno ${tiempo.interno}" else "Colectivo en camino",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Arribo estimado",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RiojaOrange.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, RiojaOrange.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = tiempo.formatearTiempo(),
                                        color = RiojaOrangeLight,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botón para cerrar
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = RiojaOrange,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Cerrar",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Crea la fuente de tiles de OSMDroid usando urlPrincipalMapas con fallback a urlSecundariaMapas o MAPNIK.
 */
private fun createTileSource(config: AppConfig?): ITileSource {
    val primaryUrl = config?.urlPrincipalMapas?.takeIf { it.isNotBlank() }
    val secondaryUrl = config?.urlSecundariaMapas?.takeIf { it.isNotBlank() }
    val targetUrl = primaryUrl ?: secondaryUrl

    if (targetUrl != null && targetUrl.contains("{z}")) {
        try {
            val baseUrl = targetUrl.substringBefore("{z}")
            return XYTileSource(
                "RiojaBusTiles",
                1, 19, 256, ".png",
                arrayOf(baseUrl)
            )
        } catch (_: Exception) {
            // fallback al mapa estándar
        }
    }
    return TileSourceFactory.MAPNIK
}

/**
 * Genera dinámicamente un ícono circular blanco con borde RiojaOrange para las paradas.
 */
private fun createStopMarkerDrawable(context: Context): Drawable {
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
private fun createBusMarkerDrawable(context: Context): Drawable {
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
 * Función de extensión para extraer recursivamente todas las líneas de un LineGroup.
 */
private fun LineGroup.extractAllLines(): List<BusLine> {
    val result = mutableListOf<BusLine>()
    result.addAll(lineas)
    subGrupos.forEach { sub ->
        result.addAll(sub.extractAllLines())
    }
    return result.distinctBy { it.codLinea }
}

/**
 * Formatea el nombre de un grupo de líneas para mostrarlo amigablemente.
 */
private fun formatGroupName(rawName: String): String {
    return rawName.replace("_", " ")
        .lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
