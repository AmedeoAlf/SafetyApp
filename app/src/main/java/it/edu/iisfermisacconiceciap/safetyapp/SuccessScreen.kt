package it.edu.iisfermisacconiceciap.safetyapp

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.SafetyAppTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.Date
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@Composable
fun StatusWidget() {
    var dotColor by remember { mutableStateOf(Color.Gray) }
    var h1 by remember { mutableStateOf("Recupero informazioni") }
    var h2 by remember { mutableStateOf("Solo un secondo") }
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .padding(8.dp)
            .width(600.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.width(5.dp))
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
                .align(Alignment.CenterVertically)
        )
        VerticalDivider(Modifier.padding(10.dp))
        Column(Modifier.padding(10.dp)) {
            Text(h1, style = MaterialTheme.typography.headlineLarge)
            Text(h2, style = MaterialTheme.typography.bodyLarge)
        }
        LaunchedEffect(Unit) {
            Util.lastThrownException.collectLatest { e ->
                when {
                    e != null -> {
                        dotColor = Color.Red
                        when (e) {
                            is ConnectException, is SocketTimeoutException -> {
                                h1 = "Impossibile connettersi al server"
                                h2 = "Assicurati di essere connesso alla rete della scuola"
                            }

                            else -> {
                                h1 = "Errore del server"
                                h2 = "Controlla informazioni di debug per altro"
                            }
                        }
                    }

                    Background.isEmergency.value == false -> {
                        dotColor = Color.Green
                        h1 = "SafetyApp è in funzione"
                        h2 = "Nessuna emergenza in corso"
                    }

                    else -> {
                        dotColor = Color.Yellow
                        h1 = "Emergenza in corso"
                        h2 = "${Background.currEmergency}\n${Background.currDescrizione}"
                    }
                }

            }
        }
    }
}

@Composable
fun StatCard(preferencesManager: PreferencesManager) {
    var lastException by remember { mutableStateOf("---") }
    var totalSuccessful by remember { mutableStateOf("---") }
    var totalUnreachable by remember { mutableStateOf("---") }
    var lastReset by remember { mutableStateOf("---") }

    Card(Modifier.padding(40.dp)) {
        Column(
            Modifier
                .padding(15.dp)
                .width(500.dp),
        ) {
            Text("Ultima eccezione: $lastException")
            Text("Connessioni avvenute con successo: $totalSuccessful")
            Text("Connessioni fallite: $totalUnreachable")
            Text("Ultimo reset: $lastReset")
            Button({
                val now = Instant.now()
                runBlocking {
                    preferencesManager.setInt("total_connections", 0)
                    preferencesManager.setInt("total_unreachable", 0)
                    preferencesManager.setInt("onStartCommand", 0)
                    preferencesManager.setInstant(
                        "lastReset", now
                    )
                }
                lastReset = now.toString()
            }) { Text("Reset statistiche") }
        }

        LaunchedEffect(lastReset) {
            Util.lastThrownException.collectLatest { e ->
                lastException = e?.message ?: "Nessuna"
                totalSuccessful = preferencesManager.getInt("total_connections").toString()
                totalUnreachable = preferencesManager.getInt("total_unreachable").toString()
                val lastResetTimestamp = preferencesManager.getInstant("lastReset")
                lastReset = if (lastResetTimestamp == null) "mai" else Date.from(
                    lastResetTimestamp
                ).toString()
            }
        }
    }
}

@Preview(group = "ok", device = "id:tv_4k")
@Preview(group = "ok", device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SuccessScreen(preferencesManager: PreferencesManager? = null) {
    var snoozeLeft by remember { mutableStateOf(Background.getSnoozeLeft()) }
    var openDialog by remember { mutableStateOf(false) }

    SafetyAppTheme {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues()),
                topBar = {
                    Column(
                        Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button({ openDialog = true }) { Text("Informazioni di debug") }
                    }
                },
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(it),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusWidget()
                    if (snoozeLeft != null) Button({
                        Background.snoozeUntil = Instant.now().plusSeconds(60 * 5)
                    }) {
                        Text("Allarme inibito per $snoozeLeft")
                    }

                    if (openDialog) Dialog(
                        { openDialog = false },
                        DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        if (preferencesManager != null) StatCard(preferencesManager)
                    }
                    LaunchedEffect(Unit) {
                        Timer().scheduleAtFixedRate(0L, 100L) {
                            snoozeLeft = Background.getSnoozeLeft()
                        }
                    }
                }
            }
        }
    }
}