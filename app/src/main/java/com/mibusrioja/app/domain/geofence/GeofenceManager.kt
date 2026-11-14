package com.mibusrioja.app.domain.geofence

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.mibusrioja.app.service.AlertForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages starting/stopping the AlertForegroundService.
 * Called when the user creates or deletes alerts.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Inicia el servicio de monitoreo. Devuelve false si no hay
     * permiso de ubicación (FINE ni COARSE) y no lo inicia.
     */
    fun startMonitoring(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return false

        val intent = Intent(context, AlertForegroundService::class.java).apply {
            action = AlertForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
        return true
    }

    fun stopMonitoring() {
        val intent = Intent(context, AlertForegroundService::class.java).apply {
            action = AlertForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
