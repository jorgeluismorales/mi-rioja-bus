package com.mibusrioja.app.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Guarda el stacktrace de cualquier crash no capturado en el almacenamiento interno
 * para poder mostrarlo/compartirlo en el próximo arranque (útil para diagnosticar sin adb).
 */
object CrashReporter {

    private const val FILE_NAME = "last_crash.txt"
    private const val ANR_THRESHOLD_MS = 5_000L

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeReport(app, "crash", thread, throwable.stackTraceToString()) }
            previous?.uncaughtException(thread, throwable)
        }
        startAnrWatchdog(app)
    }

    /**
     * Si el hilo principal no atiende un tick durante [ANR_THRESHOLD_MS], guarda su stacktrace
     * (lo que Android muestra como "la aplicación no responde") para poder diagnosticarlo sin adb.
     */
    private fun startAnrWatchdog(app: Context) {
        val mainHandler = Handler(Looper.getMainLooper())
        Thread({
            val tick = AtomicLong()
            var reported = false
            while (true) {
                val expected = tick.get() + 1
                mainHandler.post { tick.set(expected) }
                Thread.sleep(ANR_THRESHOLD_MS)
                if (tick.get() != expected) {
                    if (!reported) {
                        reported = true
                        val main = Looper.getMainLooper().thread
                        runCatching {
                            writeReport(
                                app, "ANR (hilo principal bloqueado > ${ANR_THRESHOLD_MS / 1000}s)", main,
                                main.stackTrace.joinToString("\n") { " at $it" }
                            )
                        }
                    }
                } else {
                    reported = false
                }
            }
        }, "anr-watchdog").apply { isDaemon = true }.start()
    }

    private fun writeReport(app: Context, title: String, thread: Thread, trace: String) {
        val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        File(app.filesDir, FILE_NAME).writeText(
            buildString {
                appendLine("Mi Bus Rioja - $title $stamp")
                appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Thread: ${thread.name}")
                appendLine()
                append(trace)
            }
        )
    }

    fun readLastCrash(context: Context): String? =
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    fun shareIntent(report: String): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mi Bus Rioja - reporte de error")
            putExtra(Intent.EXTRA_TEXT, report)
        },
        "Compartir reporte"
    )
}
