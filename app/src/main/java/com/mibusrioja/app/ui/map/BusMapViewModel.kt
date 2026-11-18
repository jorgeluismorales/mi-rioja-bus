package com.mibusrioja.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.domain.model.AppConfig
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.BusPosition
import com.mibusrioja.app.domain.model.LineGroup
import com.mibusrioja.app.domain.model.RouteNode
import com.mibusrioja.app.domain.model.TiempoEstimado
import com.mibusrioja.app.domain.usecase.GetBusPositionsUseCase
import com.mibusrioja.app.domain.usecase.GetConfigUseCase
import com.mibusrioja.app.domain.usecase.GetLinesUseCase
import com.mibusrioja.app.data.local.UserPreferencesRepository
import com.mibusrioja.app.data.map.TileSourceProvider
import com.mibusrioja.app.domain.usecase.GetArrivalTimesUseCase
import com.mibusrioja.app.domain.usecase.GetRouteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusMapViewModel @Inject constructor(
    private val getLinesUseCase: GetLinesUseCase,
    private val getConfigUseCase: GetConfigUseCase,
    private val getRouteUseCase: GetRouteUseCase,
    private val getBusPositionsUseCase: GetBusPositionsUseCase,
    private val getArrivalTimesUseCase: GetArrivalTimesUseCase,
    private val tileSourceProvider: TileSourceProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _lineGroups = MutableStateFlow<List<LineGroup>>(emptyList())
    val lineGroups: StateFlow<List<LineGroup>> = _lineGroups.asStateFlow()

    private val _selectedLine = MutableStateFlow<BusLine?>(null)
    val selectedLine: StateFlow<BusLine?> = _selectedLine.asStateFlow()

    private val _routeNodes = MutableStateFlow<List<RouteNode>>(emptyList())
    val routeNodes: StateFlow<List<RouteNode>> = _routeNodes.asStateFlow()

    private val _busPositions = MutableStateFlow<List<BusPosition>>(emptyList())
    val busPositions: StateFlow<List<BusPosition>> = _busPositions.asStateFlow()

    private val _showStops = MutableStateFlow(true)
    val showStops: StateFlow<Boolean> = _showStops.asStateFlow()

    private val _onlyRampa = MutableStateFlow(false)
    val onlyRampa: StateFlow<Boolean> = _onlyRampa.asStateFlow()

    val visibleBuses: StateFlow<List<BusPosition>> = combine(_busPositions, _onlyRampa) { buses, onlyRampa ->
        if (onlyRampa) buses.filter { it.tieneRampa } else buses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedBus = MutableStateFlow<BusPosition?>(null)
    val selectedBus: StateFlow<BusPosition?> = _selectedBus.asStateFlow()

    private val _lastBusUpdateMillis = MutableStateFlow<Long?>(null)
    val lastBusUpdateMillis: StateFlow<Long?> = _lastBusUpdateMillis.asStateFlow()

    private val _selectedStop = MutableStateFlow<RouteNode?>(null)
    val selectedStop: StateFlow<RouteNode?> = _selectedStop.asStateFlow()

    private val _stopArrivalTimes = MutableStateFlow<List<TiempoEstimado>>(emptyList())
    val stopArrivalTimes: StateFlow<List<TiempoEstimado>> = _stopArrivalTimes.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingArrivals = MutableStateFlow<Boolean>(false)
    val isLoadingArrivals: StateFlow<Boolean> = _isLoadingArrivals.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _appConfig = MutableStateFlow<AppConfig?>(null)
    val appConfig: StateFlow<AppConfig?> = _appConfig.asStateFlow()

    private val _tileUrl = MutableStateFlow<String?>(null)
    val tileUrl: StateFlow<String?> = _tileUrl.asStateFlow()

    private var busPollingJob: Job? = null

    init {
        loadInitialData()
    }

    /**
     * Carga la configuración inicial (centro del mapa, tiles) y los grupos de líneas.
     */
    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // 1. Cargar configuración general
            getConfigUseCase().fold(
                onSuccess = { config ->
                    _appConfig.value = config
                    _tileUrl.value = tileSourceProvider.resolve(config)
                },
                onFailure = { e ->
                    _error.value = "Error al cargar la configuración: ${e.localizedMessage ?: "Error de red"}"
                }
            )

            // 2. Cargar grupos de líneas
            getLinesUseCase().fold(
                onSuccess = { groups ->
                    _lineGroups.value = groups
                    restoreLastLine(groups)
                },
                onFailure = { e ->
                    _error.value = "Error al cargar las líneas: ${e.localizedMessage ?: "Error de red"}"
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Al abrir la app vuelve a seleccionar la última línea filtrada por el usuario.
     */
    private suspend fun restoreLastLine(groups: List<LineGroup>) {
        if (_selectedLine.value != null) return
        val code = userPreferencesRepository.lastLineCode.first() ?: return
        groups.asSequence().flatMap { flattenLines(it).asSequence() }
            .firstOrNull { it.codLinea == code }
            ?.let { selectLine(it) }
    }

    private fun flattenLines(group: LineGroup): List<BusLine> =
        group.lineas + group.subGrupos.flatMap { flattenLines(it) }

    /**
     * Selecciona una línea de colectivos, descarga los nodos del recorrido
     * y comienza el sondeo periódico de posiciones cada 15 segundos.
     */
    fun selectLine(line: BusLine) {
        if (_selectedLine.value?.codLinea == line.codLinea && _routeNodes.value.isNotEmpty()) {
            return
        }

        // Cancelar polling anterior si existía
        cancelPolling()

        _selectedLine.value = line
        viewModelScope.launch { userPreferencesRepository.saveLastLineCode(line.codLinea) }
        _selectedStop.value = null
        _stopArrivalTimes.value = emptyList()
        _busPositions.value = emptyList()
        _lastBusUpdateMillis.value = null
        _routeNodes.value = emptyList()
        _error.value = null

        viewModelScope.launch {
            _isLoading.value = true
            getRouteUseCase(line.codLinea).fold(
                onSuccess = { nodes ->
                    _routeNodes.value = nodes
                    _isLoading.value = false
                    startPollingBuses(line.codLinea)
                },
                onFailure = { e ->
                    _isLoading.value = false
                    _error.value = "Error al obtener recorrido de ${line.descripcion}: ${e.localizedMessage ?: "Error de red"}"
                }
            )
        }
    }

    /**
     * Inicia el polling continuo de posiciones en tiempo real cada 15 segundos.
     */
    private fun startPollingBuses(codLinea: String) {
        cancelPolling()
        busPollingJob = viewModelScope.launch {
            try {
                while (isActive) {
                    getBusPositionsUseCase(codLinea).fold(
                        onSuccess = { positions ->
                            _busPositions.value = positions
                            _lastBusUpdateMillis.value = System.currentTimeMillis()
                            _selectedBus.value = _selectedBus.value
                                ?.let { current -> positions.firstOrNull { it.interno == current.interno } }
                        },
                        onFailure = {
                            // En fallos intermitentes de red durante polling no bloqueamos la UI
                        }
                    )
                    delay(POLLING_INTERVAL_MS)
                }
            } catch (_: CancellationException) {
                // Cancelación intencional al cambiar de línea o cerrar ViewModel
            }
        }
    }

    /**
     * Selecciona una parada para consultar tiempos estimados de llegada.
     */
    fun selectStop(stop: RouteNode) {
        _selectedStop.value = stop
        _stopArrivalTimes.value = emptyList()

        val codLinea = _selectedLine.value?.codLinea
        val codParada = stop.codParada

        if (codLinea != null && !codParada.isNullOrBlank()) {
            viewModelScope.launch {
                _isLoadingArrivals.value = true
                getArrivalTimesUseCase(codLinea, codParada).fold(
                    onSuccess = { times ->
                        _stopArrivalTimes.value = times
                    },
                    onFailure = { e ->
                        _error.value = "No se pudieron obtener arribos para la parada: ${e.localizedMessage ?: "Error de red"}"
                    }
                )
                _isLoadingArrivals.value = false
            }
        }
    }

    /**
     * Deselecciona la parada activa y limpia los tiempos de arribo.
     */
    fun setShowStops(show: Boolean) {
        _showStops.value = show
    }

    fun setOnlyRampa(only: Boolean) {
        _onlyRampa.value = only
    }

    fun selectBus(interno: String) {
        _selectedBus.value = _busPositions.value.firstOrNull { it.interno == interno }
    }

    fun clearSelectedBus() {
        _selectedBus.value = null
    }

    fun clearSelectedStop() {
        _selectedStop.value = null
        _stopArrivalTimes.value = emptyList()
    }

    /**
     * Limpia el mensaje de error activo.
     */
    fun clearError() {
        _error.value = null
    }

    private fun cancelPolling() {
        busPollingJob?.cancel()
        busPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelPolling()
    }

    companion object {
        private const val POLLING_INTERVAL_MS = 1_000L
    }
}
