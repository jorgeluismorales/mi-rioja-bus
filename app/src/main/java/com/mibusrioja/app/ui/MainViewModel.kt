package com.mibusrioja.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.domain.model.AppConfig
import com.mibusrioja.app.domain.usecase.GetAlertsUseCase
import com.mibusrioja.app.domain.usecase.GetConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val getConfigUseCase: GetConfigUseCase
) : ViewModel() {

    // Count of active alerts monitored in real-time
    val alertCount: StateFlow<Int> = getAlertsUseCase()
        .map { alerts -> alerts.count { it.activa } }
        .catch { emit(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Remote app configuration loaded at startup
    private val _appConfig = MutableStateFlow<AppConfig?>(null)
    val appConfig: StateFlow<AppConfig?> = _appConfig.asStateFlow()

    private val _isLoadingConfig = MutableStateFlow(false)
    val isLoadingConfig: StateFlow<Boolean> = _isLoadingConfig.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _isLoadingConfig.value = true
            getConfigUseCase()
                .onSuccess { config ->
                    _appConfig.value = config
                    _isLoadingConfig.value = false
                }
                .onFailure {
                    _isLoadingConfig.value = false
                }
        }
    }
}
