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
import com.mibusrioja.app.data.local.AlertEntity
import com.mibusrioja.app.data.repository.AlertRepository
import com.mibusrioja.app.domain.geo.BusTracker
import com.mibusrioja.app.domain.geo.GeoUtils
import com.mibusrioja.app.domain.geo.RouteProgress
import com.mibusrioja.app.domain.usecase.GetBusPositionsUseCase
import com.mibusrioja.app.domain.usecase.GetRouteUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Foreground service que monitorea la ubicación GPS del usuario
 * y la posición de los colectivos para disparar alertas de proximidad.
 */
@AndroidEntryPoint
class AlertForegroundService : Service() {

    companion object {
        const val TAG = "AlertForegroundService"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val LOCATION_INTERVAL_MS = 5_000L
        private const val LOCATION_FASTEST_MS = 2_000L
        private const val BUS_POLL_INTERVAL_MS = 1_000L
    }

    @Inject lateinit var alertRepository: AlertRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var getBusPositionsUseCase: GetBusPositionsUseCase
    @Inject lateinit var getRouteUseCase: GetRouteUseCase

    /** Recorrido de cada línea con alertas (se carga una vez; viene de la caché diaria). */
    private val routeProgress = mutableMapOf<String, RouteProgress>()
    private val busTrackers = mutableMapOf<String, BusTracker>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var busPollingJob: Job? = null
    private var monitoring = false

    @Volatile private var activeAlerts: List<AlertEntity> = emptyList()

    // Track which alerts have already fired (to avoid spam)
    private val firedAlerts = mutableSetOf<Long>()
    private val firedBusAlerts = mutableSetOf<Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location -> checkAlerts(location) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (!hasLocationPermission()) {
                    Log.w(TAG, "Location permission not granted, stopping service")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun startMonitoring() {
        startForeground(
            NotificationHelper.NOTIF_SERVICE_ID,
            notificationHelper.buildServiceNotification()
        )
        if (monitoring) return
        monitoring = true
        observeAlerts()
        startBusPolling()
        requestLocationUpdates()
    }

    private fun observeAlerts() {
        serviceScope.launch {
            alertRepository.getAllAlerts().collect { alerts ->
                val actives = alerts.filter { it.activa }
                activeAlerts = actives
                updateServiceNotification(actives.size)
                if (actives.isEmpty()) {
                    Log.d(TAG, "No active alerts left, stopping service")
                    stopSelf()
                }
            }
        }
    }

    private fun updateServiceNotification(count: Int) {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(
            NotificationHelper.NOTIF_SERVICE_ID,
            notificationHelper.buildServiceNotification(count)
        )
    }

    private fun startBusPolling() {
        busPollingJob?.cancel()
        busPollingJob = serviceScope.launch {
            try {
                while (isActive) {
                    checkBusProximity()
                    delay(BUS_POLL_INTERVAL_MS)
                }
            } catch (_: CancellationException) {
            }
        }
    }

    private suspend fun routeFor(codLinea: String): RouteProgress? =
        routeProgress[codLinea] ?: getRouteUseCase(codLinea).getOrNull()
            ?.takeIf { it.size >= 2 }
            ?.let { RouteProgress(it) }
            ?.also { routeProgress[codLinea] = it }

    /**
     * Dispara la alerta sólo si algún colectivo **se acerca** a la parada: está detrás de ella
     * en el sentido del recorrido y le faltan ≤ distancia metros por la traza. Un colectivo que
     * ya pasó (aunque siga a pocos metros en línea recta) no avisa.
     */
    private suspend fun checkBusProximity() {
        val alerts = activeAlerts
        if (alerts.isEmpty()) return

        val lineas = alerts.map { it.codLinea }.distinct()
        for (codLinea in lineas) {
            val positions = getBusPositionsUseCase(codLinea).getOrNull() ?: continue
            val route = routeFor(codLinea)
            val tracker = route?.let { busTrackers.getOrPut(codLinea) { BusTracker(it) } }
            tracker?.retainBuses(positions.map { it.interno }.toSet())
            // Puntos del recorrido donde puede estar cada colectivo (vacío = fuera de ruta)
            val located = tracker?.let { t ->
                positions.map { t.locate(it.interno, it.lat, it.lng, it.orientacion.toDouble()) }
            }
            val alertsDeLinea = alerts.filter { it.codLinea == codLinea }
            for (alert in alertsDeLinea) {
                val stopIdx = route?.stopIndex(alert.codParada, alert.latParada, alert.lngParada)

                // Para cada colectivo: metros que le faltan hasta la parada (null = ya pasó / fuera de ruta)
                val approaching = positions.mapIndexedNotNull { i, bus ->
                    val remaining = if (route != null && located != null && stopIdx != null) {
                        located[i]
                            .filterNot { route.hasPassed(it, stopIdx) }
                            .mapNotNull { route.metersToStop(it, stopIdx) }
                            .minOrNull() ?: return@mapIndexedNotNull null
                    } else {
                        // Sin recorrido disponible: distancia en línea recta (comportamiento anterior)
                        GeoUtils.distanceMeters(bus.lat, bus.lng, alert.latParada, alert.lngParada)
                    }
                    bus to remaining
                }
                val closest = approaching.minByOrNull { it.second }

                if (closest != null && closest.second <= alert.distanciaMetros) {
                    if (alert.id !in firedBusAlerts) {
                        firedBusAlerts.add(alert.id)
                        val (bus, distance) = closest
                        notificationHelper.showBusAlertNotification(
                            alertId = alert.id,
                            title = "¡Tu colectivo está cerca!",
                            text = "El interno ${bus.interno} de la línea ${alert.lineaDescripcion.ifBlank { codLinea }} está a ${distance.roundToInt()} m de ${alert.paradaDescripcion}",
                            isBusTrigger = true
                        )
                        Log.d(TAG, "Bus alert fired: ${alert.id} interno ${bus.interno} at ${distance}m")
                    }
                } else if (closest == null || closest.second > alert.distanciaMetros * 2) {
                    // Ningún colectivo acercándose en rango: rearmar para el próximo
                    firedBusAlerts.remove(alert.id)
                }
            }
        }
    }

    private fun requestLocationUpdates() {
        if (!hasLocationPermission()) {
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

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                mainLooper
            )
        } catch (_: SecurityException) {
        }
    }

    private fun checkAlerts(userLocation: Location) {
        for (alert in activeAlerts) {
            val distanceMeters = GeoUtils.distanceMeters(
                userLocation.latitude, userLocation.longitude,
                alert.latParada, alert.lngParada
            )

            if (distanceMeters <= alert.distanciaMetros && alert.id !in firedAlerts) {
                firedAlerts.add(alert.id)
                val d = distanceMeters.roundToInt()
                notificationHelper.showBusAlertNotification(
                    alertId = alert.id,
                    title = if (alert.isTransbordo) "Preparate para bajar" else "Estás llegando a tu parada",
                    text = if (alert.isTransbordo) "Tu parada ${alert.paradaDescripcion} está a $d m"
                           else "Estás a $d m de ${alert.paradaDescripcion}"
                )
                Log.d(TAG, "Alert fired: ${alert.id} at ${distanceMeters}m from stop")
            } else if (distanceMeters > alert.distanciaMetros * 2) {
                firedAlerts.remove(alert.id)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoring = false
        busPollingJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
