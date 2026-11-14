package com.mibusrioja.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.BusPosition
import com.mibusrioja.app.domain.model.LineGroup
import com.mibusrioja.app.domain.model.RouteNode
import com.mibusrioja.app.domain.geo.BusClusterer
import com.mibusrioja.app.domain.geo.BusMapItem
import com.mibusrioja.app.domain.model.TiempoEstimado
import com.mibusrioja.app.ui.components.AppBottomSheet
import com.mibusrioja.app.ui.components.AppCard
import com.mibusrioja.app.ui.components.Dimens
import com.mibusrioja.app.ui.components.SectionTitle
import com.mibusrioja.app.ui.theme.Background
import com.mibusrioja.app.ui.theme.DividerColor
import com.mibusrioja.app.ui.theme.RiojaOrange
import com.mibusrioja.app.ui.theme.RiojaOrangeDark
import com.mibusrioja.app.ui.theme.RiojaOrangeLight
import com.mibusrioja.app.ui.theme.SuccessGreen
import com.mibusrioja.app.ui.theme.SurfaceCard
import com.mibusrioja.app.ui.theme.SurfaceDark
import com.mibusrioja.app.ui.theme.SurfaceVariant
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
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
    val visibleBuses by viewModel.visibleBuses.collectAsStateWithLifecycle()
    val showStops by viewModel.showStops.collectAsStateWithLifecycle()
    val onlyRampa by viewModel.onlyRampa.collectAsStateWithLifecycle()
    val selectedBus by viewModel.selectedBus.collectAsStateWithLifecycle()
    val lastBusUpdateMillis by viewModel.lastBusUpdateMillis.collectAsStateWithLifecycle()
    val selectedStop by viewModel.selectedStop.collectAsStateWithLifecycle()
    val stopArrivalTimes by viewModel.stopArrivalTimes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingArrivals by viewModel.isLoadingArrivals.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val tileUrl by viewModel.tileUrl.collectAsStateWithLifecycle()

    // MapView & Overlays references
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var locationOverlayInstance by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    val polylineRef = remember { mutableStateOf<Polyline?>(null) }
    val stopMarkers = remember { mutableListOf<Marker>() }
    val busMarkers = remember { mutableListOf<Marker>() }
    val lastBusSignature = remember { mutableStateOf("") }
    var zoomLevel by remember { mutableDoubleStateOf(DEFAULT_ZOOM) }
    val lastShowStops = remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }
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
    val lifecycleMap = mapViewInstance
    val lifecycleLocOverlay = locationOverlayInstance
    DisposableEffect(lifecycleOwner, lifecycleMap) {
        val map = lifecycleMap
        val locOverlay = lifecycleLocOverlay
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
            locOverlay?.disableMyLocation()
            map?.onDetach()
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
                        setTileSource(createTileSource(tileUrl))
                        setMultiTouchControls(true)
                        setDestroyMode(false)
                        isTilesScaledToDpi = true
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent): Boolean = false
                            override fun onZoom(event: ZoomEvent): Boolean {
                                zoomLevel = event.zoomLevel
                                return true
                            }
                        })
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
                    val targetTileSource = createTileSource(tileUrl)
                    if (mapView.tileProvider.tileSource.name() != targetTileSource.name()) {
                        mapView.setTileSource(targetTileSource)
                    }

                    // 2. Actualizar recorrido (polyline y paradas) cuando cambian los nodos
                    val currentLineCode = selectedLine?.codLinea
                    val routeChanged = currentLineCode != lastRouteLineCode.value ||
                            routeNodes.size != lastRouteNodeCount.value ||
                            showStops != lastShowStops.value

                    if (routeChanged) {
                        lastRouteLineCode.value = currentLineCode
                        lastRouteNodeCount.value = routeNodes.size
                        lastShowStops.value = showStops

                        // Remover polyline anterior
                        polylineRef.value?.let { mapView.overlays.remove(it) }
                        polylineRef.value = null

                        // Remover paradas anteriores
                        stopMarkers.forEach { mapView.overlays.remove(it) }
                        stopMarkers.clear()

                        // Limpiar colectivos anteriores al cambiar de línea
                        busMarkers.forEach { mapView.overlays.remove(it) }
                        busMarkers.clear()
                        lastBusSignature.value = ""

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
                            val paradas = if (showStops) routeNodes.filter { it.esParada } else emptyList()
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

                    // 3. Actualizar marcadores de buses con clustering por zoom
                    val items = BusClusterer.cluster(visibleBuses, zoomLevel)
                    val signature = buildString {
                        append("%.1f".format(zoomLevel))
                        items.forEach { item ->
                            when (item) {
                                is BusMapItem.Single -> append("|s:${item.bus.interno}:%.5f:%.5f".format(item.bus.lat, item.bus.lng))
                                is BusMapItem.Cluster -> append("|c:%.5f:%.5f:".format(item.lat, item.lng)).append(item.buses.joinToString(",") { it.interno })
                            }
                        }
                    }

                    if (signature != lastBusSignature.value) {
                        lastBusSignature.value = signature
                        busMarkers.forEach { mapView.overlays.remove(it) }
                        busMarkers.clear()

                        items.forEach { item ->
                            when (item) {
                                is BusMapItem.Single -> {
                                    val bus = item.bus
                                    val marker = Marker(mapView).apply {
                                        id = bus.interno
                                        position = GeoPoint(bus.lat, bus.lng)
                                        rotation = bus.orientacion
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        icon = busIconDrawable
                                        setOnMarkerClickListener { _, _ ->
                                            viewModel.selectBus(bus.interno)
                                            true
                                        }
                                    }
                                    busMarkers.add(marker)
                                    mapView.overlays.add(marker)
                                }
                                is BusMapItem.Cluster -> {
                                    val marker = Marker(mapView).apply {
                                        position = GeoPoint(item.lat, item.lng)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        icon = createClusterDrawable(context, item.buses.size)
                                        setOnMarkerClickListener { _, _ ->
                                            mapView.controller.animateTo(
                                                GeoPoint(item.lat, item.lng),
                                                (mapView.zoomLevelDouble + 2).coerceAtMost(19.0),
                                                500L
                                            )
                                            true
                                        }
                                    }
                                    busMarkers.add(marker)
                                    mapView.overlays.add(marker)
                                }
                            }
                        }
                        mapView.invalidate()
                    }
                }
            )

            // ── 2. Píldoras superiores: línea actual y acceso a filtros ─────
            if (selectedLine != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceDark.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, DividerColor),
                    onClick = { showFilters = true },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(RiojaOrange, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Línea ${selectedLine?.descripcion ?: ""}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, DividerColor),
                onClick = { showFilters = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = RiojaOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filtros",
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── 3. Badge informativo de colectivos activos ────────────────────
            if (selectedLine != null && visibleBuses.isNotEmpty()) {
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
                            text = "${visibleBuses.size} ${if (visibleBuses.size == 1) "colectivo activo" else "colectivos activos"}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── 4. Estado vacío: 'Seleccioná una línea' ───────────────────────
            if (selectedLine == null && !isLoading) {
                AppCard(
                    containerColor = SurfaceCard.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, RiojaOrange.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Row(
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
                                text = "Tocá Filtros y elegí una línea para ver su recorrido",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Vas a ver la ruta, las paradas y los colectivos activos en tiempo real.",
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
        AppBottomSheet(onDismissRequest = { viewModel.clearSelectedStop() }) {
            StopArrivalBottomSheetContent(
                stop = selectedStop!!,
                selectedLine = selectedLine,
                arrivalTimes = stopArrivalTimes,
                isLoading = isLoadingArrivals,
                onDismiss = { viewModel.clearSelectedStop() }
            )
        }
    }

    if (selectedBus != null) {
        BusDetailSheet(
            bus = selectedBus!!,
            selectedLine = selectedLine,
            lastBusUpdateMillis = lastBusUpdateMillis,
            onDismiss = { viewModel.clearSelectedBus() }
        )
    }

    if (showFilters) {
        FiltersSheet(
            lineGroups = lineGroups,
            selectedLine = selectedLine,
            showStops = showStops,
            onlyRampa = onlyRampa,
            onSelectLine = { line -> viewModel.selectLine(line); showFilters = false },
            onShowStopsChange = { viewModel.setShowStops(it) },
            onOnlyRampaChange = { viewModel.setOnlyRampa(it) },
            onDismiss = { showFilters = false }
        )
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
 * Sheet de filtros: grupo, línea y opciones de visualización del mapa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(
    lineGroups: List<LineGroup>,
    selectedLine: BusLine?,
    showStops: Boolean,
    onlyRampa: Boolean,
    onSelectLine: (BusLine) -> Unit,
    onShowStopsChange: (Boolean) -> Unit,
    onOnlyRampaChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGroupIndex by remember { mutableIntStateOf(0) }
    val currentGroup = lineGroups.getOrNull(selectedGroupIndex)
    val linesInGroup = currentGroup?.extractAllLines() ?: emptyList()

    AppBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Filtros",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(Dimens.lg))

        SectionTitle(text = "Grupo")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(lineGroups.size) { index ->
                val group = lineGroups[index]
                val isSelected = selectedGroupIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedGroupIndex = index },
                    label = { Text(formatGroupName(group.nombre)) },
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

        SectionTitle(text = "Línea")
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            items(linesInGroup, key = { it.codLinea }) { line ->
                val isSelected = line.codLinea == selectedLine?.codLinea
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    color = if (isSelected) RiojaOrange.copy(alpha = 0.15f) else SurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) RiojaOrange else DividerColor),
                    onClick = { onSelectLine(line) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isSelected) RiojaOrange else RiojaOrange.copy(alpha = 0.7f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = line.codLinea,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = line.descripcion.ifBlank { "Línea ${line.codLinea}" },
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.lg))

        SectionTitle(text = "Opciones")

        FilterSwitchRow(
            label = "Mostrar paradas",
            checked = showStops,
            onCheckedChange = onShowStopsChange
        )
        Spacer(modifier = Modifier.height(Dimens.sm))
        FilterSwitchRow(
            label = "Solo colectivos con rampa",
            checked = onlyRampa,
            onCheckedChange = onOnlyRampaChange
        )
    }
}

@Composable
private fun FilterSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RiojaOrange,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceVariant
            )
        )
    }
}

/**
 * Sheet con detalle del colectivo seleccionado en el mapa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusDetailSheet(
    bus: BusPosition,
    selectedLine: BusLine?,
    lastBusUpdateMillis: Long?,
    onDismiss: () -> Unit
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(RiojaOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    text = "Interno ${bus.interno}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Línea ${selectedLine?.descripcion ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.lg))

        val elapsedSec = lastBusUpdateMillis?.let { ((nowMillis - it) / 1000).coerceAtLeast(0) }
        val updatedText = when {
            elapsedSec == null -> "Sin dato"
            elapsedSec < 60 -> "hace ${elapsedSec}s"
            else -> "hace ${elapsedSec / 60} min"
        }
        val heading = bus.orientacion.toInt()
        val cardinal = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")[
            ((heading + 22) / 45) % 8
        ]

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
            BusInfoTile(
                label = "Próxima parada",
                value = bus.proximaParada?.takeIf { it.isNotBlank() } ?: "Sin dato",
                modifier = Modifier.weight(1f)
            )
            BusInfoTile(
                label = "Accesibilidad",
                value = if (bus.tieneRampa) "Con rampa" else "Sin rampa",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(Dimens.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
            BusInfoTile(
                label = "Rumbo",
                value = "$cardinal ${heading}°",
                modifier = Modifier.weight(1f)
            )
            BusInfoTile(
                label = "Actualizado",
                value = updatedText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.lg))

        Text(
            text = "La API de Red Bus no publica patente ni ETA por colectivo.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BusInfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radiusSm),
        color = SurfaceVariant,
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(modifier = Modifier.padding(Dimens.md)) {
            Text(
                text = label,
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Crea la fuente de tiles de OSMDroid usando urlPrincipalMapas con fallback a urlSecundariaMapas o MAPNIK.
 */
internal fun createTileSource(url: String?): ITileSource {
    val target = url?.takeIf { it.contains("{z}") } ?: return TileSourceFactory.MAPNIK
    return try {
        val host = target.substringBefore("{z}").substringAfter("://").substringBefore("/")
        XYTileSource(
            "tiles-$host",
            1, 19, 256, ".png",
            arrayOf(target.substringBefore("{z}"))
        )
    } catch (_: Exception) {
        TileSourceFactory.MAPNIK
    }
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
