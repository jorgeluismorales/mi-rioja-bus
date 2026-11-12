package com.mibusrioja.app.ui.alerts

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mibusrioja.app.domain.model.Alert
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.LineGroup
import com.mibusrioja.app.domain.model.RouteNode
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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = RiojaOrange,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar alerta"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mis Alertas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Avisos de proximidad a tus paradas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                if (alerts.isNotEmpty()) {
                    val activeCount = alerts.count { it.activa }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (activeCount > 0) RiojaOrange.copy(alpha = 0.15f) else SurfaceVariant,
                        border = BorderStroke(1.dp, if (activeCount > 0) RiojaOrange.copy(alpha = 0.4f) else DividerColor)
                    ) {
                        Text(
                            text = "$activeCount activas",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeCount > 0) RiojaOrange else TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (alerts.isEmpty()) {
                EmptyAlertsState(
                    onAddClick = { viewModel.showCreateDialog() }
                )
            } else {
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.activa) SurfaceCard.copy(alpha = 0.9f) else SurfaceDark.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (alert.activa) GlassBorder else DividerColor.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RiojaAmber.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, RiojaAmber.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = RiojaAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Transbordo automático",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RiojaAmber
                                )
                            }
                        }
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
}

@Composable
fun EmptyAlertsState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = RiojaOrange,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Todavía no tenés alertas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Creá alertas de proximidad para recibir notificaciones cuando tu colectivo se acerque a tu parada.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = RiojaOrange),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddAlert,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crear primera alerta",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
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
                    OutlinedTextField(
                        value = lineFilter,
                        onValueChange = { lineFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar línea por número o nombre...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RiojaOrange,
                            unfocusedBorderColor = DividerColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                    OutlinedTextField(
                        value = stopSearch,
                        onValueChange = { stopSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar parada por nombre o código...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RiojaOrange,
                            unfocusedBorderColor = DividerColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard
                        ),
                        shape = RoundedCornerShape(12.dp)
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
