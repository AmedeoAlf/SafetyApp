package it.edu.iisfermisacconiceciap.safetyapp

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.edu.iisfermisacconiceciap.safetyapp.FetchEmergencyService.Companion.getSnoozeLeft
import it.edu.iisfermisacconiceciap.safetyapp.FetchEmergencyService.Companion.snoozeUntil
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.SafetyAppTheme
import java.time.Instant
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@Preview(device = "id:tv_4k")
@Preview(device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmergencyScreen() {
    SafetyAppTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {


            val mod = Modifier
                .fillMaxSize()
                .safeContentPadding()
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Row(mod, Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                    EmergencyPlan(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.4f)
                    )
                    EmergencyDisplay()
                }
            } else {
                Column(mod, Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                    EmergencyPlan(
                        Modifier
                            .fillMaxHeight(0.5f)
                            .fillMaxWidth()
                    )
                    EmergencyDisplay()
                }
            }
        }
    }
}

@Composable
fun EmergencyDisplay() {
    var timeLeft by remember { mutableStateOf(getSnoozeLeft()) }
    Scaffold(
        Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                if (timeLeft != null) Text(
                    "L'allarme non verrà riaperto per $timeLeft",
                    Modifier
                        .align(Alignment.CenterVertically)
                        .padding(15.dp)
                )
                Button(
                    { snoozeUntil = Instant.now().plusSeconds(60 * 5) },
                    Modifier.padding(15.dp)
                ) {
                    Text("Ignora per 5 minuti")
                }
                LaunchedEffect(Unit) {
                    Timer().scheduleAtFixedRate(0L, 100L) {
                        timeLeft = getSnoozeLeft()
                    }
                }
            }
        }
    ) { it ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val transition = rememberInfiniteTransition()
            val blink by transition.animateFloat(
                1F, 0F,
                animationSpec = infiniteRepeatable(
                    tween(400),
                    repeatMode = RepeatMode.Reverse
                ),
            )

            Image(
                painterResource(R.drawable.warning),
                "Warning icon",
                Modifier
                    .size(120.dp)
                    .alpha(blink)
            )
            Text(
                FetchEmergencyService.lastResponse?.currEmergency ?: "---",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            Text(
                FetchEmergencyService.lastResponse?.currDescrizione ?: "---",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmergencyPlan(modifier: Modifier = Modifier) {
    Image(
        painterResource(R.drawable.emergency_plan), "Mappa piano di emergenza",
        modifier = modifier
    )
}