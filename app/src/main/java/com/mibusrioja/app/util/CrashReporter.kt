package com.mibusrioja.app.util

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Guarda el stacktrace de cualquier crash no capturado en el almacenamiento interno
 * para poder mostrarlo/compartirlo en el próximo arranque (útil para diagnosticar sin adb).
 */
object CrashReporter {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                File(app.filesDir, FILE_NAME).writeText(
                    buildString {
                        appendLine("Mi Bus Rioja - crash $stamp")
                        appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ${Build.MANUFACTURER} ${Build.MODEL}")
                        appendLine("Thread: ${thread.name}")
                        appendLine()
                        append(throwable.stackTraceToString())
                    }
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
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
