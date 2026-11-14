package com.mibusrioja.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mibusrioja.app.R
import com.mibusrioja.app.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ALERTS_ID = "mibusrioja_alerts"
        const val CHANNEL_SERVICE_ID = "mibusrioja_service"
        const val NOTIF_SERVICE_ID = 1001
        const val NOTIF_ALERT_BASE_ID = 2000
    }

    private val notifManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS_ID,
            context.getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_alerts_desc)
            enableVibration(true)
            setShowBadge(true)
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE_ID,
            context.getString(R.string.channel_service_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_service_desc)
            setShowBadge(false)
        }

        notifManager.createNotificationChannel(alertsChannel)
        notifManager.createNotificationChannel(serviceChannel)
    }

    fun buildServiceNotification(alertCount: Int = 0) = NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
        .setContentTitle("Mi Bus Rioja")
        .setContentText(
            if (alertCount > 0) "Monitoreando $alertCount alertas de colectivo"
            else "Monitoreando alertas de colectivos…"
        )
        .setSmallIcon(R.drawable.ic_bus_notification)
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(openAppIntent())
        .build()

    /**
     * Notificación de proximidad. [isBusTrigger] distingue los avisos disparados
     * por la posición del colectivo de los disparados por la ubicación del
     * usuario, para no pisar los ids de notificación.
     */
    fun showBusAlertNotification(
        alertId: Long,
        title: String,
        text: String,
        isBusTrigger: Boolean = false
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_bus_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .setContentIntent(openAppIntent())
            .build()

        val notifId = NOTIF_ALERT_BASE_ID + (alertId * 2).toInt() + if (isBusTrigger) 1 else 0
        notifManager.notify(notifId, notification)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
