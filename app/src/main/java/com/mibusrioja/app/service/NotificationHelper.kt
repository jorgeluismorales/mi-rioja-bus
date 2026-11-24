package com.mibusrioja.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
        /** El sonido de un canal no se puede cambiar una vez creado, por eso el canal nuevo. */
        const val CHANNEL_ALERTS_ID = "mibusrioja_alerts_v2"
        private const val LEGACY_CHANNEL_ALERTS_ID = "mibusrioja_alerts"
        private val ALERT_VIBRATION = longArrayOf(0, 400, 200, 400, 200, 400)
        const val CHANNEL_SERVICE_ID = "mibusrioja_service"
        const val NOTIF_SERVICE_ID = 1001
        const val NOTIF_ALERT_BASE_ID = 2000
    }

    private val notifManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        // Si el usuario había bloqueado el canal viejo, el nuevo nace bloqueado también.
        val legacyBlocked = notifManager.getNotificationChannel(LEGACY_CHANNEL_ALERTS_ID)
            ?.importance == NotificationManager.IMPORTANCE_NONE
        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS_ID,
            context.getString(R.string.channel_alerts_name),
            if (legacyBlocked) NotificationManager.IMPORTANCE_NONE else NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_alerts_desc)
            // Sonido y vibración los emite playAlarmFeedback() con uso de alarma,
            // que el modo silencio/vibración no apaga.
            setSound(null, null)
            enableVibration(false)
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

        notifManager.deleteNotificationChannel(LEGACY_CHANNEL_ALERTS_ID)
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
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppIntent())
            .build()

        val notifId = NOTIF_ALERT_BASE_ID + (alertId * 2).toInt() + if (isBusTrigger) 1 else 0
        notifManager.notify(notifId, notification)
        if (alertsChannelEnabled()) playAlarmFeedback()
    }

    private fun alertsChannelEnabled(): Boolean =
        notifManager.areNotificationsEnabled() &&
            notifManager.getNotificationChannel(CHANNEL_ALERTS_ID)
                ?.importance != NotificationManager.IMPORTANCE_NONE

    /** Suena y vibra por el canal de alarma para que se escuche aun con el teléfono en silencio. */
    private fun playAlarmFeedback() {
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        runCatching {
            RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = alarmAudio
                play()
            }
        }

        val effect = VibrationEffect.createWaveform(ALERT_VIBRATION, -1)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator().vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator().vibrate(effect, alarmAudio)
            }
        }
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
