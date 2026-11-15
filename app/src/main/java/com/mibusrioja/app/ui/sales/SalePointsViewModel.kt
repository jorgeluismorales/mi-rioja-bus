package com.mibusrioja.app.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.data.map.TileSourceProvider
import com.mibusrioja.app.domain.geo.GeoUtils
import com.mibusrioja.app.domain.model.PuntoVenta
import com.mibusrioja.app.domain.usecase.GetConfigUseCase
import com.mibusrioja.app.domain.usecase.GetPuntosVentaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SalePointsViewModel @Inject constructor(
    private val getPuntosVentaUseCase: GetPuntosVentaUseCase,
    private val getConfigUseCase: GetConfigUseCase,
    private val tileSourceProvider: TileSourceProvider
) : ViewModel() {

    private val _puntos = MutableStateFlow<List<PuntoVenta>>(emptyList())
    val puntos: StateFlow<List<PuntoVenta>> = _puntos.asStateFlow()


    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _tileUrl = MutableStateFlow<String?>(null)
    val tileUrl: StateFlow<String?> = _tileUrl.asStateFlow()

    // Ordena por proximidad si la ubicación del usuario está disponible
    val filteredPuntos: StateFlow<List<PuntoVenta>> = combine(_puntos, _userLocation) { puntosList, userLoc ->
        if (userLoc != null) {
            puntosList.sortedBy { punto ->
                GeoUtils.distanceMeters(userLoc.first, userLoc.second, punto.lat, punto.lng)
            }
        } else {
            puntosList
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadPuntos()
        resolveTiles()
    }

    private fun resolveTiles() {
        viewModelScope.launch {
            getConfigUseCase().onSuccess { config ->
                _tileUrl.value = tileSourceProvider.resolve(config)
            }
        }
    }

    /**
     * Carga inicial o recarga de todos los puntos de venta.
     * La API requiere coordenadas: usa la ubicación del usuario o el centro de La Rioja.
     */
    fun loadPuntos() {
        val loc = _userLocation.value
        loadNearby(loc?.first ?: LA_RIOJA_LAT, loc?.second ?: LA_RIOJA_LNG)
    }

    /**
     * Carga puntos de venta cercanos a las coordenadas indicadas.
     */
    fun loadNearby(lat: Double, lng: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            getPuntosVentaUseCase(lat = lat, lng = lng)
                .onSuccess { list ->
                    _puntos.value = list
                    _isLoading.value = false
                }
                .onFailure { throwable ->
                    _error.value = throwable.localizedMessage
                        ?: "No se pudieron cargar los puntos de venta"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Actualiza la ubicación del usuario para calcular distancias y ordenar puntos.
     */
    fun updateUserLocation(lat: Double, lng: Double) {
        _userLocation.value = Pair(lat, lng)
    }

    /**
     * Recarga los datos respetando la ubicación actual.
     */
    fun refresh() {
        loadPuntos()
    }

    companion object {
        private const val LA_RIOJA_LAT = -29.4134003
        private const val LA_RIOJA_LNG = -66.8560292
    }
}
