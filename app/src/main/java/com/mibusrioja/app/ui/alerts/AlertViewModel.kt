package com.mibusrioja.app.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.domain.geofence.GeofenceManager
import com.mibusrioja.app.domain.model.Alert
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.LineGroup
import com.mibusrioja.app.domain.model.RouteNode
import com.mibusrioja.app.domain.usecase.CreateAlertUseCase
import com.mibusrioja.app.domain.usecase.DeleteAlertUseCase
import com.mibusrioja.app.domain.usecase.GetAlertsUseCase
import com.mibusrioja.app.domain.usecase.GetLinesUseCase
import com.mibusrioja.app.domain.usecase.GetRouteUseCase
import com.mibusrioja.app.domain.usecase.ToggleAlertUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val createAlertUseCase: CreateAlertUseCase,
    private val deleteAlertUseCase: DeleteAlertUseCase,
    private val toggleAlertUseCase: ToggleAlertUseCase,
    private val getLinesUseCase: GetLinesUseCase,
    private val getRouteUseCase: GetRouteUseCase,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    val alerts: StateFlow<List<Alert>> = getAlertsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _lineGroups = MutableStateFlow<List<LineGroup>>(emptyList())
    val lineGroups: StateFlow<List<LineGroup>> = _lineGroups.asStateFlow()

    /** codLinea -> nombre de la línea, para mostrar las alertas con un nombre legible. */
    val lineNames: StateFlow<Map<String, String>> = _lineGroups
        .map { groups -> groups.flatMap { flattenLines(it) }.associate { it.codLinea to it.descripcion } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private fun flattenLines(group: LineGroup): List<BusLine> =
        group.lineas + group.subGrupos.flatMap { flattenLines(it) }

    private val _selectedLine = MutableStateFlow<BusLine?>(null)
    val selectedLine: StateFlow<BusLine?> = _selectedLine.asStateFlow()

    private val _stopsForLine = MutableStateFlow<List<RouteNode>>(emptyList())
    val stopsForLine: StateFlow<List<RouteNode>> = _stopsForLine.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadLineGroups()
    }

    private fun loadLineGroups() {
        viewModelScope.launch {
            getLinesUseCase()
                .onSuccess { _lineGroups.value = it }
                .onFailure { _error.value = "Error al cargar líneas: ${it.message}" }
        }
    }

    fun selectLineForAlert(line: BusLine) {
        _selectedLine.value = line
        _stopsForLine.value = emptyList()
        viewModelScope.launch {
            getRouteUseCase(line.codLinea)
                .onSuccess { nodes ->
                    _stopsForLine.value = nodes.filter { it.esParada }
                }
                .onFailure {
                    _error.value = "Error al cargar paradas de la línea: ${it.message}"
                }
        }
    }

    fun createAlert(
        codParada: String,
        paradaDesc: String,
        lat: Double,
        lng: Double,
        distanciaMetros: Int
    ) {
        val line = _selectedLine.value ?: run {
            _error.value = "No se ha seleccionado ninguna línea"
            return
        }

        viewModelScope.launch {
            try {
                val alert = Alert(
                    codLinea = line.codLinea,
                    lineaDescripcion = line.descripcion,
                    codParada = codParada,
                    paradaDescripcion = paradaDesc,
                    distanciaMetros = distanciaMetros,
                    latParada = lat,
                    lngParada = lng,
                    activa = true,
                    isTransbordo = false
                )
                createAlertUseCase(alert)
                if (!geofenceManager.startMonitoring()) {
                    _error.value = "Concedé el permiso de ubicación para activar las alertas"
                }
                hideCreateDialog()
            } catch (e: Exception) {
                _error.value = "Error al crear alerta: ${e.message}"
            }
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch {
            try {
                deleteAlertUseCase(id)
            } catch (e: Exception) {
                _error.value = "Error al eliminar alerta: ${e.message}"
            }
        }
    }

    fun toggleAlert(id: Long, activa: Boolean) {
        viewModelScope.launch {
            try {
                toggleAlertUseCase(id, activa)
                if (activa) {
                    geofenceManager.startMonitoring()
                }
            } catch (e: Exception) {
                _error.value = "Error al cambiar estado de alerta: ${e.message}"
            }
        }
    }

    fun showCreateDialog() {
        _isCreating.value = true
    }

    fun hideCreateDialog() {
        _isCreating.value = false
        _selectedLine.value = null
        _stopsForLine.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }
}
