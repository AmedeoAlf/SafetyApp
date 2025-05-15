package it.edu.iisfermisacconiceciap.safetyapp

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.edu.iisfermisacconiceciap.safetyapp.Background.Companion.snoozeUntil
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.SafetyAppTheme
import kotlinx.coroutines.delay
import java.time.Instant

@Preview(device = "id:tv_4k")
@Preview(device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmergencyScreen() {
    SafetyAppTheme {
        var emergency by remember { mutableStateOf(Background.currEmergency) }
        var desc by remember { mutableStateOf(Background.currDescrizione) }
        var warning by remember { mutableStateOf(Background.isEmergency) }
        var timeLeft by remember { mutableStateOf(Background.getSnoozeLeft()) }
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Button({ snoozeUntil = Instant.now().plusSeconds(60 * 5) }) {
                    Text(
                        timeLeft ?: "Ignora per 5 minuti"
                    )
                }
                Column(
                    Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (warning) Image(
                        painterResource(R.drawable.warning),
                        "Warning icon",
                        Modifier.size(120.dp)
                    )
                    Text(
                        emergency,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        desc, style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center
                    )
                    LaunchedEffect(key1 = Unit, block = {
                        while (true) {
                            delay(200)
                            emergency = Background.currEmergency
                            desc = Background.currDescrizione
                            warning = Background.isEmergency
                            timeLeft = Background.getSnoozeLeft()
                        }
                    })

                }
            }
        }
    }
}
