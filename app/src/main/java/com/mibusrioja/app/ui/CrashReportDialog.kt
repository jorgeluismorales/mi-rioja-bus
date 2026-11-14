package com.mibusrioja.app.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibusrioja.app.util.CrashReporter

/**
 * Si la app se cerró por un error en la sesión anterior, muestra el stacktrace
 * con opción de compartirlo (permite diagnosticar sin acceso a logcat).
 */
@Composable
fun CrashReportDialog() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashReporter.readLastCrash(context)) }
    val current = report ?: return

    fun dismiss() {
        CrashReporter.clear(context)
        report = null
    }

    AlertDialog(
        onDismissRequest = { dismiss() },
        title = { Text("La app se cerró por un error") },
        text = {
            Text(
                text = current,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = { context.startActivity(CrashReporter.shareIntent(current)) }) {
                Text("Compartir")
            }
        },
        dismissButton = {
            TextButton(onClick = { dismiss() }) { Text("Cerrar") }
        }
    )
}
