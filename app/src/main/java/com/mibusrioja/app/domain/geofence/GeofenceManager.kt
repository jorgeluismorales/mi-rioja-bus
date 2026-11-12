package com.mibusrioja.app.domain.geofence

import android.content.Context
import android.content.Intent
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
    fun startMonitoring() {
        val intent = Intent(context, AlertForegroundService::class.java).apply {
            action = AlertForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopMonitoring() {
        val intent = Intent(context, AlertForegroundService::class.java).apply {
            action = AlertForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
