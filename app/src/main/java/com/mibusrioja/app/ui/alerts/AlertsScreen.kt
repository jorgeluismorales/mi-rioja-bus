package com.mibusrioja.app.ui.alerts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mibusrioja.app.domain.model.Alert
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.LineGroup
import com.mibusrioja.app.domain.model.RouteNode
import com.mibusrioja.app.ui.components.AppBottomSheet
import com.mibusrioja.app.ui.components.AppCard
import com.mibusrioja.app.ui.components.AppTextField
import com.mibusrioja.app.ui.components.Dimens
import com.mibusrioja.app.ui.components.EmptyState
import com.mibusrioja.app.ui.components.ErrorBanner
import com.mibusrioja.app.ui.components.PrimaryButton
import com.mibusrioja.app.ui.components.ScreenHeader
import com.mibusrioja.app.ui.components.StatusPill
import com.mibusrioja.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val lineGroups by viewModel.lineGroups.collectAsStateWithLifecycle()
    val selectedLine by viewModel.selectedLine.collectAsStateWithLifecycle()
    val stopsForLine by viewModel.stopsForLine.collectAsStateWithLifecycle()
    val isCreating by viewModel.isCreating.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Pedir permisos de ubicación antes de abrir el sheet de creación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.showCreateDialog()
    }

    fun requestCreateAlert() {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            viewModel.showCreateDialog()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(Dimens.lg))

            ScreenHeader(
                title = "Mis Alertas",
                subtitle = "Avisos de proximidad a tus paradas",
                trailing = {
                    if (alerts.isNotEmpty()) {
                        val activeCount = alerts.count { it.activa }
                        StatusPill(
                            text = "$activeCount activas",
                            color = if (activeCount > 0) RiojaOrange else TextMuted
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(Dimens.lg))

            if (error != null) {
                ErrorBanner(
                    message = error.orEmpty(),
                    onDismiss = { viewModel.clearError() }
                )
                Spacer(modifier = Modifier.height(Dimens.md))
            }

            if (alerts.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.DirectionsBus,
                    title = "Todavía no tenés alertas",
                    message = "Te avisamos cuando vos o tu colectivo se acerquen a la parada que elijas.",
                    actionLabel = "Crear primera alerta de proximidad",
                    onAction = { requestCreateAlert() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                PrimaryButton(
                    text = "Nueva alerta",
                    onClick = { requestCreateAlert() },
                    icon = Icons.Default.Add
                )
                Spacer(modifier = Modifier.height(Dimens.md))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = alerts,
                        key = { it.id }
                    ) { alert ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
                        ) {
                            AlertCard(
                                alert = alert,
                                onToggle = { activa -> viewModel.toggleAlert(alert.id, activa) },
                                onDelete = { viewModel.deleteAlert(alert.id) }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Sheet para crear nueva alerta
        if (isCreating) {
            CreateAlertBottomSheet(
                lineGroups = lineGroups,
                selectedLine = selectedLine,
                stopsForLine = stopsForLine,
                onDismiss = { viewModel.hideCreateDialog() },
                onSelectLine = { line -> viewModel.selectLineForAlert(line) },
                onConfirmAlert = { codParada, descParada, lat, lng, dist ->
                    viewModel.createAlert(codParada, descParada, lat, lng, dist)
                }
            )
        }
    }
}

@Composable
fun AlertCard(
    alert: Alert,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        containerColor = if (alert.activa) SurfaceCard else SurfaceDark.copy(alpha = 0.6f),
        border = BorderStroke(
            width = 1.dp,
            color = if (alert.activa) GlassBorder else DividerColor.copy(alpha = 0.4f)
        )
    ) {
            // Fila superior: Línea, Transbordo badge, Botón eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip de Línea
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RiojaOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, RiojaOrange.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Línea ${alert.codLinea}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RiojaOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Badge de Transbordo Automático si aplica
                    if (alert.isTransbordo) {
                        StatusPill(
                            text = "Transbordo automático",
                            color = RiojaAmber
                        )
                    }
                }

                // Botón eliminar
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar alerta",
                        tint = ErrorRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nombre de la parada
            Text(
                text = alert.paradaDescripcion.ifBlank { "Parada ${alert.codParada}" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (alert.activa) TextPrimary else TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (alert.lineaDescripcion.isNotBlank()) {
                Text(
                    text = alert.lineaDescripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila inferior: Umbral de distancia + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (alert.activa) RiojaOrange else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Aviso a ${alert.distanciaMetros}m de la parada",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (alert.activa) TextSecondary else TextMuted
                    )
                }

                Switch(
                    checked = alert.activa,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RiojaOrange,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceVariant,
                        uncheckedBorderColor = DividerColor
                    )
                )
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlertBottomSheet(
    lineGroups: List<LineGroup>,
    selectedLine: BusLine?,
    stopsForLine: List<RouteNode>,
    onDismiss: () -> Unit,
    onSelectLine: (BusLine) -> Unit,
    onConfirmAlert: (codParada: String, paradaDesc: String, lat: Double, lng: Double, distanciaMetros: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedStop by remember { mutableStateOf<RouteNode?>(null) }
    var selectedDistance by remember { mutableIntStateOf(100) }

    // Helper para aplanar líneas de un grupo y sus subgrupos
    fun flattenLines(group: LineGroup): List<BusLine> {
        return group.lineas + group.subGrupos.flatMap { flattenLines(it) }
    }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
            // Cabecera con indicador de pasos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nueva Alerta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Indicador de pasos (1, 2, 3)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepBadge(stepNumber = 1, currentStep = currentStep, label = "Línea")
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(1.dp)
                            .background(if (currentStep > 1) RiojaOrange else DividerColor)
                    )
                    StepBadge(stepNumber = 2, currentStep = currentStep, label = "Parada")
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(1.dp)
                            .background(if (currentStep > 2) RiojaOrange else DividerColor)
                    )
                    StepBadge(stepNumber = 3, currentStep = currentStep, label = "Distancia")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (currentStep) {
                // ── PASO 1: Seleccionar línea ──────────────────────────────────────────
                1 -> {
                    Text(
                        text = "Seleccioná la línea de colectivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    var selectedGroupIndex by remember { mutableIntStateOf(0) }
                    val currentGroup = lineGroups.getOrNull(selectedGroupIndex)
                    val linesInGroup = currentGroup?.let { flattenLines(it) } ?: emptyList()

                    if (lineGroups.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(lineGroups.size) { index ->
                                val group = lineGroups[index]
                                val isSelected = selectedGroupIndex == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedGroupIndex = index },
                                    label = { Text(group.nombre) },
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

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    var lineFilter by remember { mutableStateOf("") }
                    AppTextField(
                        value = lineFilter,
                        onValueChange = { lineFilter = it },
                        placeholder = "Buscar línea por número o nombre...",
                        leadingIcon = Icons.Default.Search
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredLines = linesInGroup.filter {
                        lineFilter.isBlank() ||
                                it.codLinea.contains(lineFilter, ignoreCase = true) ||
                                it.descripcion.contains(lineFilter, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLines) { line ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectLine(line)
                                        currentStep = 2
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCard,
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RiojaOrange.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, RiojaOrange.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = line.codLinea,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = RiojaOrange,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = line.descripcion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (line.grupo.isNotBlank()) {
                                            Text(
                                                text = line.grupo,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // ── PASO 2: Seleccionar parada ─────────────────────────────────────────
                2 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Línea ${selectedLine?.codLinea ?: ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RiojaOrange
                            )
                            Text(
                                text = "Seleccioná la parada a vigilar",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        TextButton(onClick = { currentStep = 1 }) {
                            Text("Cambiar línea", color = RiojaOrange, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    var stopSearch by remember { mutableStateOf("") }
                    AppTextField(
                        value = stopSearch,
                        onValueChange = { stopSearch = it },
                        placeholder = "Buscar parada por nombre o código...",
                        leadingIcon = Icons.Default.Place
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (stopsForLine.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = RiojaOrange, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Cargando paradas de la línea...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        val filteredStops = stopsForLine.filter {
                            stopSearch.isBlank() ||
                                    (it.nombreParada?.contains(stopSearch, ignoreCase = true) == true) ||
                                    (it.codParada?.contains(stopSearch, ignoreCase = true) == true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredStops) { stop ->
                                val isSelected = selectedStop?.codParada == stop.codParada && stop.codParada != null
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedStop = stop
                                            currentStep = 3
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) RiojaOrange.copy(alpha = 0.15f) else SurfaceCard,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) RiojaOrange else GlassBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsBus,
                                            contentDescription = null,
                                            tint = if (isSelected) RiojaOrange else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stop.nombreParada ?: "Parada ${stop.codParada ?: ""}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (stop.codParada != null) {
                                                Text(
                                                    text = "Código: ${stop.codParada}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── PASO 3: Distancia y Confirmación ────────────────────────────────────
                3 -> {
                    Text(
                        text = "Distancia de notificación",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Te avisaremos cuando el colectivo esté a esta distancia de tu parada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val distances = listOf(
                        50 to "50 metros (Muy cerca)",
                        100 to "100 metros (Recomendado)",
                        200 to "200 metros (Con anticipación)",
                        500 to "500 metros (Distancia amplia)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        distances.forEach { (dist, desc) ->
                            val isSelected = selectedDistance == dist
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDistance = dist },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) RiojaOrange.copy(alpha = 0.15f) else SurfaceCard,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) RiojaOrange else GlassBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedDistance = dist },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = RiojaOrange,
                                            unselectedColor = TextMuted
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Resumen de la alerta
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceVariant,
                        border = BorderStroke(1.dp, DividerColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Resumen de alerta",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = RiojaOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Línea: ${selectedLine?.descripcion ?: selectedLine?.codLinea ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Parada: ${selectedStop?.nombreParada ?: selectedStop?.codParada ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Distancia de aviso: $selectedDistance metros",
                                style = MaterialTheme.typography.bodySmall,
                                color = RiojaOrangeLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 2 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DividerColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Atrás")
                        }

                        Button(
                            onClick = {
                                val stop = selectedStop
                                if (stop != null) {
                                    onConfirmAlert(
                                        stop.codParada ?: "PARADA",
                                        stop.nombreParada ?: "Parada de colectivo",
                                        stop.lat,
                                        stop.lng,
                                        selectedDistance
                                    )
                                }
                            },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RiojaOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Crear Alerta",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun StepBadge(
    stepNumber: Int,
    currentStep: Int,
    label: String
) {
    val isCurrent = stepNumber == currentStep
    val isDone = stepNumber < currentStep

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> SuccessGreen
                        isCurrent -> RiojaOrange
                        else -> SurfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color.White else TextMuted
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) RiojaOrange else if (isDone) TextPrimary else TextMuted
        )
    }
}
