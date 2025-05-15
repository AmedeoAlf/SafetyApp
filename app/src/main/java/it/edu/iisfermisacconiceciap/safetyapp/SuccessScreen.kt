package it.edu.iisfermisacconiceciap.safetyapp

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.SafetyAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.Date

@Preview(group = "ok", device = "id:tv_4k")
@Preview(group = "ok", device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SuccessScreen(preferencesManager: PreferencesManager? = null) {
    var totalSuccessful by remember { mutableStateOf("---") }
    var totalUnreachable by remember { mutableStateOf("---") }
    var snoozeLeft by remember { mutableStateOf(Background.getSnoozeLeft()) }
    var lastReset by remember { mutableStateOf("---") }
    SafetyAppTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (snoozeLeft != null) Button({
                    Background.snoozeUntil = Instant.now().plusSeconds(60 * 5)
                }) {
                    Text("Allarme inibito per $snoozeLeft")
                }
                Text("OK", style = MaterialTheme.typography.displayLarge)
                Text("Tutti i permessi in regola")
                Text("Connessioni avvenute con successo: $totalSuccessful")
                Text("Connessioni fallite: $totalUnreachable")
                LaunchedEffect(lastReset) {
                    while (true) {
                        totalSuccessful = preferencesManager?.getInt("total_connections").toString()
                        totalUnreachable =
                            preferencesManager?.getInt("total_unreachable").toString()
                        val lastResetTimestamp = preferencesManager?.getInstant("lastReset")
                        lastReset =
                            if (lastResetTimestamp == null) "mai" else Date.from(lastResetTimestamp)
                                .toString()
                        snoozeLeft = Background.getSnoozeLeft()
                        delay(100)
                    }
                }
                Button({
                    val now = Instant.now()
                    runBlocking {
                        preferencesManager?.setInt("total_connections", 0)
                        preferencesManager?.setInt("total_unreachable", 0)
                        preferencesManager?.setInt("onStartCommand", 0)
                        preferencesManager?.setInstant(
                            "lastReset", now
                        )
                    }
                    lastReset = now.toString()
                }) { Text("Reset statistiche") }
                Text("Last reset: $lastReset")
            }
        }
    }
}
