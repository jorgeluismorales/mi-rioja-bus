package com.mibusrioja.app.ui.tariffs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.domain.model.TarifaItem
import com.mibusrioja.app.domain.usecase.GetConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TarifasViewModel @Inject constructor(
    private val getConfigUseCase: GetConfigUseCase
) : ViewModel() {

    private val _tarifas = MutableStateFlow<List<TarifaItem>>(emptyList())
    val tarifas: StateFlow<List<TarifaItem>> = _tarifas.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTarifas()
    }

    fun loadTarifas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            getConfigUseCase()
                .onSuccess { config ->
                    _tarifas.value = config.tarifas
                    _isLoading.value = false
                }
                .onFailure { throwable ->
                    _error.value = throwable.localizedMessage ?: "Error al cargar las tarifas vigentes"
                    _isLoading.value = false
                }
        }
    }

    fun load() = loadTarifas()

    fun refresh() = loadTarifas()
}
