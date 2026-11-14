package com.mibusrioja.app.ui.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.data.local.UserPreferencesRepository
import com.mibusrioja.app.data.repository.BusRepository
import com.mibusrioja.app.domain.model.Saldo
import com.mibusrioja.app.domain.model.SaldoException
import com.mibusrioja.app.domain.usecase.CheckBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaldoViewModel @Inject constructor(
    private val checkBalanceUseCase: CheckBalanceUseCase,
    private val busRepository: BusRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _cardNumber = MutableStateFlow("")
    val cardNumber: StateFlow<String> = _cardNumber.asStateFlow()

    private val _captchaCode = MutableStateFlow("")
    val captchaCode: StateFlow<String> = _captchaCode.asStateFlow()

    private val _captchaImage = MutableStateFlow<ByteArray?>(null)
    val captchaImage: StateFlow<ByteArray?> = _captchaImage.asStateFlow()

    private val _isLoadingCaptcha = MutableStateFlow(false)
    val isLoadingCaptcha: StateFlow<Boolean> = _isLoadingCaptcha.asStateFlow()

    private val _saldo = MutableStateFlow<Saldo?>(null)
    val saldo: StateFlow<Saldo?> = _saldo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveCard = MutableStateFlow(false)
    val saveCard: StateFlow<Boolean> = _saveCard.asStateFlow()

    private val _showVerification = MutableStateFlow(false)
    val showVerification: StateFlow<Boolean> = _showVerification.asStateFlow()

    init {
        loadCaptcha()
        viewModelScope.launch {
            val saved = userPreferencesRepository.savedCardNumber.first()
            if (saved != null) {
                _cardNumber.value = saved
                _saveCard.value = true
            }
        }
    }

    fun setSaveCard(enabled: Boolean) {
        _saveCard.value = enabled
        if (!enabled) {
            viewModelScope.launch {
                userPreferencesRepository.clearCardNumber()
            }
        }
    }

    fun requestVerification() {
        val number = _cardNumber.value.trim()
        if (number.isBlank()) {
            _error.value = "Ingresá el número de tu tarjeta para consultar"
            return
        }
        if (number.length < 5) {
            _error.value = "El número ingresado es muy corto. Verificá los datos"
            return
        }
        _error.value = null
        _showVerification.value = true
        if (_captchaImage.value == null) {
            loadCaptcha()
        }
    }

    fun dismissVerification() {
        _showVerification.value = false
        _error.value = null
    }

    /**
     * Descarga la imagen de captcha asociada a la sesión actual.
     */
    fun loadCaptcha() {
        viewModelScope.launch {
            _isLoadingCaptcha.value = true
            busRepository.getCaptcha()
                .onSuccess { _captchaImage.value = it }
                .onFailure { _captchaImage.value = null }
            _isLoadingCaptcha.value = false
        }
    }

    /**
     * Actualiza el número de tarjeta ingresado por el usuario,
     * admitiendo sólo dígitos numéricos para evitar caracteres inválidos.
     */
    fun updateCardNumber(number: String) {
        val digitsOnly = number.filter { it.isDigit() }.take(16)
        _cardNumber.value = digitsOnly
        if (_error.value != null) {
            _error.value = null
        }
    }

    /**
     * Actualiza el código de captcha ingresado por el usuario.
     */
    fun updateCaptcha(captcha: String) {
        _captchaCode.value = captcha
        if (_error.value != null) {
            _error.value = null
        }
    }

    /**
     * Limpia mensajes de error activos.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Limpia la consulta actual para iniciar una nueva.
     */
    fun clearSaldo() {
        _saldo.value = null
        _error.value = null
    }

    /**
     * Realiza la consulta de saldo anónima a través del caso de uso.
     */
    fun checkBalance() {
        val number = _cardNumber.value.trim()
        if (number.isBlank()) {
            _error.value = "Ingresá el número de tu tarjeta para consultar"
            return
        }
        if (number.length < 5) {
            _error.value = "El número ingresado es muy corto. Verificá los datos"
            return
        }
        val captcha = _captchaCode.value.trim()
        if (captcha.isBlank()) {
            _error.value = "Ingresá el código de seguridad de la imagen"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            checkBalanceUseCase(nroTarjeta = number, captcha = captcha)
                .onSuccess { result ->
                    _saldo.value = result
                    _isLoading.value = false
                    _showVerification.value = false
                    if (_saveCard.value) {
                        userPreferencesRepository.saveCardNumber(number)
                    }
                    // El captcha es de un solo uso: recargar para la próxima consulta
                    _captchaCode.value = ""
                    loadCaptcha()
                }
                .onFailure { throwable ->
                    _error.value = throwable.localizedMessage
                        ?: "No se pudo obtener el saldo. Comprobá el número de tarjeta o tu conexión"
                    _isLoading.value = false
                    _captchaCode.value = ""
                    loadCaptcha()
                }
        }
    }
}
