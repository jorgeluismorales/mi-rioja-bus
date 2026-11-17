package com.mibusrioja.app.ui.sales

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Atm
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.mibusrioja.app.domain.geo.GeoUtils
import com.mibusrioja.app.domain.model.PuntoVenta
import com.mibusrioja.app.domain.model.TipoPuntoVenta
import com.mibusrioja.app.ui.theme.Background
import com.mibusrioja.app.ui.theme.DividerColor
import com.mibusrioja.app.ui.theme.GlassBackground
import com.mibusrioja.app.ui.theme.GlassBorder
import com.mibusrioja.app.ui.theme.InfoBlue
import com.mibusrioja.app.ui.theme.RiojaOrange
import com.mibusrioja.app.ui.theme.SuccessGreen
import com.mibusrioja.app.ui.theme.SurfaceCard
import com.mibusrioja.app.ui.theme.SurfaceDark
import com.mibusrioja.app.ui.theme.SurfaceVariant
import com.mibusrioja.app.ui.theme.TextMuted
import com.mibusrioja.app.ui.theme.TextPrimary
import com.mibusrioja.app.ui.theme.TextSecondary
import com.mibusrioja.app.ui.theme.WarningYellow
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import com.mibusrioja.app.ui.map.createTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

private const val LA_RIOJA_LAT = -29.4134003
private const val LA_RIOJA_LNG = -66.8560292

@Composable
fun SalePointsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: SalePointsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val filteredPuntos by viewModel.filteredPuntos.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val tileUrl by viewModel.tileUrl.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var selectedPunto by remember { mutableStateOf<PuntoVenta?>(null) }

    // Launcher de permisos de ubicación para cálculo de distancia en tiempo real
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            obtainUserLocation(context) { lat, lng ->
                viewModel.updateUserLocation(lat, lng)
                viewModel.loadNearby(lat, lng)
                mapViewRef?.controller?.animateTo(GeoPoint(lat, lng))
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            obtainUserLocation(context) { lat, lng ->
                viewModel.updateUserLocation(lat, lng)
                viewModel.loadNearby(lat, lng)
            }
        }
    }

    // Ciclo de vida del MapView
    // Leer el valor en composición: el cuerpo corre después del factory y vería el mapa nuevo con clave vieja
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

    // Actualización reactiva de marcadores en el mapa
    LaunchedEffect(filteredPuntos, userLocation, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        if (map.tileRequestCompleteHandler == null) return@LaunchedEffect // MapView ya destruido (onDetach)
        map.overlays.removeAll { it is Marker }

        // Marcadores de puntos de venta coloreados por tipo
        filteredPuntos.forEach { punto ->
            val marker = Marker(map).apply {
                position = GeoPoint(punto.lat, punto.lng)
                title = punto.nombre
                snippet = "${punto.direccion}\n${punto.descripcionTipo}"
                icon = createSalePointMarkerDrawable(context, punto.tipo)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ ->
                    selectedPunto = punto
                    showInfoWindow()
                    val index = filteredPuntos.indexOf(punto)
                    if (index >= 0) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
                    true
                }
            }
            map.overlays.add(marker)
        }

        // Marcador distintivo de la posición GPS del usuario
        userLocation?.let { (uLat, uLng) ->
            val userMarker = Marker(map).apply {
                position = GeoPoint(uLat, uLng)
                title = "Tu ubicación"
                icon = createUserLocationDrawable(context)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            map.overlays.add(userMarker)
        }

        map.invalidate()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── MAPA EN TIEMPO REAL (40% de altura de la pantalla) ────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.40f)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(createTileSource(tileUrl))
                            setMultiTouchControls(true)
                            setDestroyMode(false)
                            isTilesScaledToDpi = true
                            controller.setZoom(14.0)
                            controller.setCenter(GeoPoint(LA_RIOJA_LAT, LA_RIOJA_LNG))
                            mapViewRef = this
                        }
                    },
                    update = { mapView ->
                        val target = createTileSource(tileUrl)
                        if (mapView.tileProvider.tileSource.name() != target.name()) {
                            mapView.setTileSource(target)
                        }
                    }
                )

                // Botón volver (arriba a la izquierda sobre el mapa)
                FloatingMapButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                )

                // Overlay de gradiente superior para integrar visualmente la barra de estado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Background.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                )

                // Botones flotantes del mapa (Mi Ubicación y Recentrar La Rioja)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón Recentrar Centro de La Rioja
                    FloatingMapButton(
                        icon = Icons.Rounded.CenterFocusStrong,
                        contentDescription = "Centrar La Rioja",
                        onClick = {
                            mapViewRef?.controller?.apply {
                                setZoom(14.0)
                                animateTo(GeoPoint(LA_RIOJA_LAT, LA_RIOJA_LNG))
                            }
                        }
                    )

                    // Botón Mi Ubicación
                    FloatingMapButton(
                        icon = Icons.Rounded.MyLocation,
                        contentDescription = "Mi ubicación",
                        tint = RiojaOrange,
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineGranted || coarseGranted) {
                                obtainUserLocation(context) { lat, lng ->
                                    viewModel.updateUserLocation(lat, lng)
                                    mapViewRef?.controller?.apply {
                                        setZoom(16.0)
                                        animateTo(GeoPoint(lat, lng))
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )
                }

                // Loading spinner mientras se consultan puntos
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDark.copy(alpha = 0.85f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = RiojaOrange,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Actualizando puntos...",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // ── LISTA DE PUNTOS DE VENTA (60% de altura de la pantalla) ───────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.60f)
                    .background(Background)
            ) {
                SalePointsHeader(totalCount = filteredPuntos.size)

                // Lista scrolleable de tarjetas
                if (filteredPuntos.isEmpty() && !isLoading) {
                    SalePointsEmptyState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPuntos, key = { "${it.nombre}_${it.lat}_${it.lng}" }) { punto ->
                            val distanceMeters = userLocation?.let { (uLat, uLng) ->
                                calculateDistanceMeters(uLat, uLng, punto.lat, punto.lng)
                            }

                            PuntoVentaCard(
                                punto = punto,
                                isSelected = punto == selectedPunto,
                                distanceMeters = distanceMeters,
                                onClick = {
                                    selectedPunto = punto
                                    mapViewRef?.controller?.apply {
                                        setZoom(16.5)
                                        animateTo(GeoPoint(punto.lat, punto.lng))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Encabezado de la lista con el total de puntos disponibles.
 */
@Composable
fun SalePointsHeader(totalCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Puntos de recarga y venta",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$totalCount disponibles",
            color = TextMuted,
            fontSize = 12.sp
        )
    }
}

/**
 * Tarjeta de punto de venta: icono por tipo, nombre, dirección, horario y distancia al usuario.
 */
@Composable
fun PuntoVentaCard(
    punto: PuntoVenta,
    isSelected: Boolean,
    distanceMeters: Double?,
    onClick: () -> Unit
) {
    val (typeColor, typeIcon) = when (punto.tipo) {
        TipoPuntoVenta.ATM -> WarningYellow to Icons.Rounded.Atm
        TipoPuntoVenta.COMUN -> SuccessGreen to Icons.Rounded.Storefront
        TipoPuntoVenta.ABONO -> InfoBlue to Icons.Rounded.Badge
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) SurfaceVariant else SurfaceCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) typeColor else GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icono del tipo y detalles textuales
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Badge circular del tipo
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = punto.nombre,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Dirección
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = punto.direccion,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Distancia estimada calculada
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (distanceMeters != null) {
                    Text(
                        text = formatDistance(distanceMeters),
                        color = RiojaOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Botón flotante estilizado para controlar el mapa OSMDroid.
 */
@Composable
private fun FloatingMapButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(SurfaceDark.copy(alpha = 0.92f))
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Estado vacío cuando no hay puntos coincidentes con el filtro.
 */
@Composable
private fun SalePointsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.LocationSearching,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No se encontraron puntos de recarga",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "No hay puntos de venta disponibles cerca de tu ubicación.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
    }
}

// ── Helpers gráficos para marcadores en el mapa ───────────────────────────────

/**
 * Genera un pin vectorial nítido para el mapa según el tipo:
 * ATM = Amarillo, COMUN = Verde, ABONO = Azul.
 */
private fun createSalePointMarkerDrawable(context: Context, tipo: TipoPuntoVenta): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val colorInt = when (tipo) {
        TipoPuntoVenta.ATM -> WarningYellow.toArgb()
        TipoPuntoVenta.COMUN -> SuccessGreen.toArgb()
        TipoPuntoVenta.ABONO -> InfoBlue.toArgb()
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = size / 2f

    // Fondo del círculo con el color del tipo
    paint.style = Paint.Style.FILL
    paint.color = colorInt
    canvas.drawCircle(radius, radius, radius - 3, paint)

    // Borde blanco exterior para alto contraste sobre el mapa
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f * density
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(radius, radius, radius - 3, paint)

    // Punto central blanco
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(radius, radius, radius / 3.2f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Genera el marcador azul representativo de la ubicación del usuario.
 */
private fun createUserLocationDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = size / 2f

    // Anillo halo translúcido
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.argb(80, 59, 130, 246)
    canvas.drawCircle(radius, radius, radius - 2, paint)

    // Punto azul central
    paint.color = android.graphics.Color.rgb(59, 130, 246)
    canvas.drawCircle(radius, radius, radius / 2f, paint)

    // Borde blanco interior
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f * density
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(radius, radius, radius / 2f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun obtainUserLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                onLocation(loc.latitude, loc.longitude)
            }
        }
    } catch (_: SecurityException) {
    }
}

private fun calculateDistanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double = GeoUtils.distanceMeters(lat1, lon1, lat2, lon2)

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) {
        "${meters.toInt()} m"
    } else {
        String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
    }
}
