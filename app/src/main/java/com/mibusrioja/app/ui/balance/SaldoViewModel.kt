package com.mibusrioja.app.ui.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibusrioja.app.domain.model.Saldo
import com.mibusrioja.app.domain.usecase.CheckBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaldoViewModel @Inject constructor(
    private val checkBalanceUseCase: CheckBalanceUseCase
) : ViewModel() {

    private val _cardNumber = MutableStateFlow("")
    val cardNumber: StateFlow<String> = _cardNumber.asStateFlow()

    private val _captchaCode = MutableStateFlow("")
    val captchaCode: StateFlow<String> = _captchaCode.asStateFlow()

    private val _saldo = MutableStateFlow<Saldo?>(null)
    val saldo: StateFlow<Saldo?> = _saldo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
     * Actualiza el código de captcha opcional si fuese requerido.
     */
    fun updateCaptcha(captcha: String) {
        _captchaCode.value = captcha
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

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val captchaParam = _captchaCode.value.trim().takeIf { it.isNotEmpty() }

            checkBalanceUseCase(nroTarjeta = number, captcha = captchaParam)
                .onSuccess { result ->
                    _saldo.value = result
                    _isLoading.value = false
                }
                .onFailure { throwable ->
                    _error.value = throwable.localizedMessage
                        ?: "No se pudo obtener el saldo. Comprobá el número de tarjeta o tu conexión"
                    _isLoading.value = false
                }
        }
    }
}
