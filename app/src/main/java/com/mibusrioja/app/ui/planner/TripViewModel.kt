package com.mibusrioja.app.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.data.map.TileSourceProvider
import com.mibusrioja.app.data.repository.DestinationHistoryRepository
import com.mibusrioja.app.domain.geofence.GeofenceManager
import com.mibusrioja.app.domain.model.Alert
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.LineGroup
import com.mibusrioja.app.domain.model.FrequentDestination
import com.mibusrioja.app.domain.model.PlaceSuggestion
import com.mibusrioja.app.domain.model.TripPlan
import com.mibusrioja.app.domain.model.alertTargets
import com.mibusrioja.app.domain.model.hasActiveAlerts
import com.mibusrioja.app.domain.usecase.CreateAlertUseCase
import com.mibusrioja.app.domain.usecase.FindExistingAlertUseCase
import com.mibusrioja.app.domain.usecase.GetAlertsUseCase
import com.mibusrioja.app.domain.usecase.GetConfigUseCase
import com.mibusrioja.app.domain.usecase.GetLinesUseCase
import com.mibusrioja.app.domain.usecase.PlanTripUseCase
import com.mibusrioja.app.domain.usecase.SearchPlacesUseCase
import com.mibusrioja.app.domain.usecase.ToggleAlertUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DestinationPreset(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double
)

// Coordenadas tomadas de OpenStreetMap (Nominatim)
val RIOJA_DESTINATIONS = listOf(
    DestinationPreset("Plaza 25 de Mayo", "Centro cívico, La Rioja", -29.4127171, -66.8559256),
    DestinationPreset("UNLaR", "Universidad Nacional de La Rioja", -29.4305503, -66.8705263),
    DestinationPreset("Terminal de Ómnibus", "Av. General Artigas", -29.4408842, -66.8610517),
    DestinationPreset("Hospital Regional Enrique Vera Barros", "Av. Ortiz de Ocampo", -29.4135923, -66.8350013),
    DestinationPreset("Parque de la Ciudad", "Av. Circunvalación", -29.4410119, -66.8954177),
    DestinationPreset("Hospital de la Madre y el Niño", "Av. 1° de Marzo", -29.4308454, -66.8422401),
    DestinationPreset("Paseo Cultural Castro Barros", "Pelagio B. Luna 750", -29.4118725, -66.8587962),
    DestinationPreset("Estadio Carlos Augusto Mercado Luna", "Vargas, La Rioja", -29.3954090, -66.8425408)
)

@HiltViewModel
class TripViewModel @Inject constructor(
    private val planTripUseCase: PlanTripUseCase,
    private val createAlertUseCase: CreateAlertUseCase,
    private val findExistingAlertUseCase: FindExistingAlertUseCase,
    private val getAlertsUseCase: GetAlertsUseCase,
    private val toggleAlertUseCase: ToggleAlertUseCase,
    private val getLinesUseCase: GetLinesUseCase,
    private val searchPlacesUseCase: SearchPlacesUseCase,
    private val getConfigUseCase: GetConfigUseCase,
    private val tileSourceProvider: TileSourceProvider,
    private val geofenceManager: GeofenceManager,
    private val destinationHistoryRepository: DestinationHistoryRepository
) : ViewModel() {

    private val _tripPlan = MutableStateFlow<TripPlan?>(null)
    val tripPlan: StateFlow<TripPlan?> = _tripPlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _originLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val originLocation: StateFlow<Pair<Double, Double>?> = _originLocation.asStateFlow()

    private val _destDescription = MutableStateFlow("")
    val destDescription: StateFlow<String> = _destDescription.asStateFlow()

    private val _destLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val destLocation: StateFlow<Pair<Double, Double>?> = _destLocation.asStateFlow()

    private val _suggestions = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val suggestions: StateFlow<List<PlaceSuggestion>> = _suggestions.asStateFlow()

    /** Sigue las alertas guardadas: si se elimina o pausa una, el viaje deja de figurar con alertas activadas. */
    val tripAlertsActive: StateFlow<Boolean> =
        combine(_tripPlan, getAlertsUseCase()) { plan, alerts -> plan?.hasActiveAlerts(alerts) == true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _alertsActivated = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    /** Cantidad de alertas recién activadas; se emite una sola vez por activación. */
    val alertsActivated: SharedFlow<Int> = _alertsActivated.asSharedFlow()

    private val _availableLines = MutableStateFlow<List<BusLine>>(emptyList())
    val availableLines: StateFlow<List<BusLine>> = _availableLines.asStateFlow()

    private val _tileUrl = MutableStateFlow<String?>(null)
    val tileUrl: StateFlow<String?> = _tileUrl.asStateFlow()

    val frequentDestinations: StateFlow<List<FrequentDestination>> =
        destinationHistoryRepository.frequent
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _originLabel = MutableStateFlow("Tu ubicación actual")
    val originLabel: StateFlow<String> = _originLabel.asStateFlow()

    private val _originIsGps = MutableStateFlow(true)
    val originIsGps: StateFlow<Boolean> = _originIsGps.asStateFlow()

    private val _originSuggestions = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val originSuggestions: StateFlow<List<PlaceSuggestion>> = _originSuggestions.asStateFlow()

    private var destDetail: String = ""

    private var searchJob: Job? = null
    private var originSearchJob: Job? = null

    init {
        loadLines()
        resolveTiles()
    }

    private fun loadLines() {
        viewModelScope.launch {
            getLinesUseCase().onSuccess { groups ->
                _availableLines.value = groups.flatMap { flattenLines(it) }
            }
        }
    }

    private fun resolveTiles() {
        viewModelScope.launch {
            getConfigUseCase().onSuccess { config ->
                _tileUrl.value = tileSourceProvider.resolve(config)
            }
        }
    }

    private fun flattenLines(group: LineGroup): List<BusLine> {
        return group.lineas + group.subGrupos.flatMap { flattenLines(it) }
    }

    fun setOriginFromGps(lat: Double, lng: Double) {
        useGpsOrigin(lat, lng)
    }

    fun useGpsOrigin(lat: Double, lng: Double) {
        _originLocation.value = Pair(lat, lng)
        _originLabel.value = "Tu ubicación actual"
        _originIsGps.value = true
    }

    fun onOriginQueryChanged(text: String) {
        originSearchJob?.cancel()
        originSearchJob = debouncedSearch(text, _originSuggestions)
    }

    fun selectOriginSuggestion(suggestion: PlaceSuggestion) {
        _originLocation.value = Pair(suggestion.lat, suggestion.lng)
        _originLabel.value = suggestion.nombre
        _originIsGps.value = false
        _originSuggestions.value = emptyList()
    }

    fun clearOriginSuggestions() {
        _originSuggestions.value = emptyList()
    }

    fun setDestination(description: String, lat: Double, lng: Double, detail: String = "") {
        _destDescription.value = description
        _destLocation.value = Pair(lat, lng)
        destDetail = detail
        _suggestions.value = emptyList()
    }

    fun onDestinationTextChanged(text: String) {
        _destDescription.value = text

        val match = RIOJA_DESTINATIONS.firstOrNull { it.title.equals(text.trim(), ignoreCase = true) }
        if (match != null) {
            _destLocation.value = Pair(match.lat, match.lng)
            destDetail = match.subtitle
        } else if (_destLocation.value != null && text.isBlank()) {
            _destLocation.value = null
        }

        searchJob?.cancel()
        searchJob = debouncedSearch(text, _suggestions)
    }

    private fun debouncedSearch(
        text: String,
        target: MutableStateFlow<List<PlaceSuggestion>>
    ): Job? {
        if (text.trim().length < 3) {
            target.value = emptyList()
            return null
        }
        return viewModelScope.launch {
            delay(350)
            searchPlacesUseCase(text.trim())
                .onSuccess { target.value = it }
                .onFailure { target.value = emptyList() }
        }
    }

    fun selectSuggestion(suggestion: PlaceSuggestion) {
        _destDescription.value = suggestion.nombre
        _destLocation.value = Pair(suggestion.lat, suggestion.lng)
        destDetail = suggestion.detalle
        _suggestions.value = emptyList()
    }

    fun selectFrequent(destination: FrequentDestination) {
        // Un preset guardado con coordenadas anteriores se corrige al usarlo
        val preset = RIOJA_DESTINATIONS.firstOrNull { it.title.equals(destination.nombre, ignoreCase = true) }
        if (preset == null) {
            setDestination(destination.nombre, destination.lat, destination.lng, destination.detalle)
            return
        }
        setDestination(preset.title, preset.lat, preset.lng, destination.detalle)
        viewModelScope.launch {
            destinationHistoryRepository.relocate(destination.key, preset.lat, preset.lng)
        }
    }

    fun removeFrequent(key: String) {
        viewModelScope.launch {
            destinationHistoryRepository.remove(key)
        }
    }

    fun planTrip() {
        val origin = _originLocation.value
        if (origin == null) {
            _error.value = "Esperando señal de GPS para determinar tu ubicación de origen"
            return
        }

        var dest = _destLocation.value
        if (dest == null) {
            val query = _destDescription.value.trim()
            val matchedPreset = RIOJA_DESTINATIONS.firstOrNull {
                it.title.contains(query, ignoreCase = true) || query.contains(it.title, ignoreCase = true)
            }
            if (matchedPreset != null) {
                dest = Pair(matchedPreset.lat, matchedPreset.lng)
                _destLocation.value = dest
            } else {
                _error.value = "Por favor seleccioná un destino de la lista o escribí un lugar conocido en La Rioja"
                return
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            planTripUseCase(
                originLat = origin.first,
                originLng = origin.second,
                destLat = dest.first,
                destLng = dest.second
            ).fold(
                onSuccess = { plan ->
                    _tripPlan.value = plan
                    destinationHistoryRepository.registerVisit(
                        nombre = _destDescription.value,
                        detalle = destDetail,
                        lat = dest.first,
                        lng = dest.second
                    )
                },
                onFailure = { throwable ->
                    _error.value = if (throwable is kotlinx.coroutines.TimeoutCancellationException) {
                        "La búsqueda tardó demasiado. Probá con un destino más cercano o reintentá."
                    } else {
                        throwable.message ?: "No se pudo calcular la ruta para tu viaje"
                    }
                }
            )

            _isLoading.value = false
        }
    }

    private val _isActivatingAlerts = MutableStateFlow(false)
    val isActivatingAlerts: StateFlow<Boolean> = _isActivatingAlerts.asStateFlow()

    /**
     * Activa, a pedido del usuario, las alertas de proximidad del viaje planificado
     * (transbordos + parada final). Antes se creaban solas al planificar, lo que sorprendía al usuario.
     */
    fun activateTripAlerts() {
        val plan = _tripPlan.value ?: return
        if (_isActivatingAlerts.value) return
        if (!geofenceManager.hasLocationPermission()) {
            onAlertsPermissionDenied()
            return
        }
        viewModelScope.launch {
            _isActivatingAlerts.value = true
            val activated = createAutoAlerts(plan)
            if (activated > 0) _alertsActivated.tryEmit(activated)
            _isActivatingAlerts.value = false
        }
    }

    /**
     * Crea alertas de 100 m en cada transbordo y en la parada
     * de bajada final, evitando duplicar alertas existentes
     * y reactivando las que estaban pausadas.
     */
    private suspend fun createAutoAlerts(plan: TripPlan): Int {
        var activated = 0
        var alreadyActive = false

        for ((line, stop) in plan.alertTargets()) {
            val codParada = stop.codParada ?: continue
            try {
                val existing = findExistingAlertUseCase(line.codLinea, codParada)
                if (existing != null) {
                    if (!existing.activa) {
                        toggleAlertUseCase(existing.id, true)
                        activated++
                    } else {
                        alreadyActive = true
                    }
                    continue
                }
                createAlertUseCase(
                    Alert(
                        codLinea = line.codLinea,
                        lineaDescripcion = line.descripcion,
                        codParada = codParada,
                        paradaDescripcion = stop.nombreParada ?: "Parada de transbordo",
                        distanciaMetros = 100,
                        latParada = stop.lat,
                        lngParada = stop.lng,
                        activa = true,
                        isTransbordo = true
                    )
                )
                activated++
            } catch (_: Exception) {
            }
        }

        if (activated > 0 || alreadyActive) {
            geofenceManager.startMonitoring()
        }
        return activated
    }

    fun clearPlan() {
        _tripPlan.value = null
        _error.value = null
    }

    /** Sin permiso el servicio de alertas se detiene; al recuperarlo hay que volver a iniciarlo. */
    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            if (getAlertsUseCase().first().any { it.activa }) geofenceManager.startMonitoring()
        }
    }

    fun onAlertsPermissionDenied() {
        _error.value = "Concedé el permiso de ubicación para activar las alertas del viaje"
    }

    fun clearError() {
        _error.value = null
    }
}
