package com.mibusrioja.app.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.domain.model.Alert
import com.mibusrioja.app.domain.model.BusLine
import com.mibusrioja.app.domain.model.LineGroup
import com.mibusrioja.app.domain.model.TripPlan
import com.mibusrioja.app.domain.model.TripStepType
import com.mibusrioja.app.domain.usecase.CreateAlertUseCase
import com.mibusrioja.app.domain.usecase.GetLinesUseCase
import com.mibusrioja.app.domain.usecase.PlanTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DestinationPreset(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double
)

val RIOJA_DESTINATIONS = listOf(
    DestinationPreset("Plaza 25 de Mayo", "Centro cívico, La Rioja", -29.4134003, -66.8560292),
    DestinationPreset("UNLaR", "Universidad Nacional de La Rioja", -29.434720, -66.871530),
    DestinationPreset("Terminal de Ómnibus", "Av. General Artigas", -29.428310, -66.842720),
    DestinationPreset("Hospital Regional Enrique Vera Barros", "Av. Ortiz de Ocampo", -29.418240, -66.878540),
    DestinationPreset("Parque de la Ciudad", "Av. Circunvalación", -29.448500, -66.892000),
    DestinationPreset("Hospital de la Madre y el Niño", "Av. 1° de Marzo", -29.426500, -66.862200),
    DestinationPreset("Paseo Cultural Castro Barros", "Pelagio B. Luna 750", -29.414000, -66.856800),
    DestinationPreset("Estadio Carlos Augusto Mercado Luna", "Vargas, La Rioja", -29.399500, -66.845600)
)

@HiltViewModel
class TripViewModel @Inject constructor(
    private val planTripUseCase: PlanTripUseCase,
    private val createAlertUseCase: CreateAlertUseCase,
    private val getLinesUseCase: GetLinesUseCase
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

    private val _alertsAutoCreated = MutableStateFlow(0)
    val alertsAutoCreated: StateFlow<Int> = _alertsAutoCreated.asStateFlow()

    private val _availableLines = MutableStateFlow<List<BusLine>>(emptyList())
    val availableLines: StateFlow<List<BusLine>> = _availableLines.asStateFlow()

    init {
        loadLines()
    }

    private fun loadLines() {
        viewModelScope.launch {
            getLinesUseCase().onSuccess { groups ->
                _availableLines.value = groups.flatMap { flattenLines(it) }
            }
        }
    }

    private fun flattenLines(group: LineGroup): List<BusLine> {
        return group.lineas + group.subGrupos.flatMap { flattenLines(it) }
    }

    fun setOriginFromGps(lat: Double, lng: Double) {
        _originLocation.value = Pair(lat, lng)
    }

    fun setDestination(description: String, lat: Double, lng: Double) {
        _destDescription.value = description
        _destLocation.value = Pair(lat, lng)
    }

    fun setDestinationDescription(description: String) {
        _destDescription.value = description
        // Si el texto coincide exactamente con algún preset, asociamos automáticamente las coordenadas
        val match = RIOJA_DESTINATIONS.firstOrNull { it.title.equals(description.trim(), ignoreCase = true) }
        if (match != null) {
            _destLocation.value = Pair(match.lat, match.lng)
        } else if (_destLocation.value != null && description.isBlank()) {
            _destLocation.value = null
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
            _alertsAutoCreated.value = 0

            planTripUseCase(
                originLat = origin.first,
                originLng = origin.second,
                destLat = dest.first,
                destLng = dest.second
            ).fold(
                onSuccess = { plan ->
                    _tripPlan.value = plan

                    // Auto-crear alertas de 100m para pasos de TRANSBORDO
                    var autoCreatedCount = 0
                    val steps = plan.pasos
                    for (i in steps.indices) {
                        val step = steps[i]
                        if (step.tipo == TripStepType.TRANSBORDO) {
                            val transferStop = step.paradaOrigen
                            val nextStepWithLine = steps.drop(i + 1).firstOrNull { it.linea != null }
                            val busLine = nextStepWithLine?.linea ?: step.linea

                            if (transferStop != null) {
                                try {
                                    val alert = Alert(
                                        codLinea = busLine?.codLinea ?: "TRANSBORDO",
                                        lineaDescripcion = busLine?.descripcion ?: "Combinación de línea",
                                        codParada = transferStop.codParada ?: "TRANSBORDO",
                                        paradaDescripcion = transferStop.nombreParada ?: "Parada de transbordo",
                                        distanciaMetros = 100, // Alerta automática a 100 metros
                                        latParada = transferStop.lat,
                                        lngParada = transferStop.lng,
                                        activa = true,
                                        isTransbordo = true
                                    )
                                    createAlertUseCase(alert)
                                    autoCreatedCount++
                                } catch (e: Exception) {
                                    // Continuar con el resto de alertas
                                }
                            }
                        }
                    }
                    _alertsAutoCreated.value = autoCreatedCount
                },
                onFailure = { throwable ->
                    _error.value = throwable.message ?: "No se pudo calcular la ruta para tu viaje"
                }
            )

            _isLoading.value = false
        }
    }

    fun clearPlan() {
        _tripPlan.value = null
        _alertsAutoCreated.value = 0
        _error.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
