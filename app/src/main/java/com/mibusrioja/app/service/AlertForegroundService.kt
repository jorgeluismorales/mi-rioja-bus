package com.mibusrioja.app.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.mibusrioja.app.data.repository.AlertRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Foreground service que monitorea la ubicación GPS del usuario
 * y dispara notificaciones cuando está a X metros de una parada de alerta.
 */
@AndroidEntryPoint
class AlertForegroundService : Service() {

    companion object {
        const val TAG = "AlertForegroundService"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val LOCATION_INTERVAL_MS = 10_000L   // cada 10s
        private const val LOCATION_FASTEST_MS = 5_000L
    }

    @Inject lateinit var alertRepository: AlertRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track which alerts have already fired (to avoid spam)
    private val firedAlerts = mutableSetOf<Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch { checkAlerts(location) }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        startForeground(
            NotificationHelper.NOTIF_SERVICE_ID,
            notificationHelper.buildServiceNotification()
        )
        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted, stopping service")
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            LOCATION_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_MS)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            mainLooper
        )
    }

    private suspend fun checkAlerts(userLocation: Location) {
        val activeAlerts = alertRepository.getActiveAlerts()
        for (alert in activeAlerts) {
            val stopLocation = Location("stop").apply {
                latitude = alert.latParada
                longitude = alert.lngParada
            }
            val distanceMeters = userLocation.distanceTo(stopLocation)

            if (distanceMeters <= alert.distanciaMetros && alert.id !in firedAlerts) {
                firedAlerts.add(alert.id)
                notificationHelper.showBusAlertNotification(
                    alertId = alert.id,
                    lineaDesc = "${alert.codLinea} - ${alert.lineaDescripcion}",
                    paradaDesc = alert.paradaDescripcion,
                    distanciaMetros = distanceMeters.roundToInt()
                )
                Log.d(TAG, "Alert fired: ${alert.id} at ${distanceMeters}m from stop")
            } else if (distanceMeters > alert.distanciaMetros * 2) {
                // Reset fired state once user moves away (2x threshold)
                firedAlerts.remove(alert.id)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
