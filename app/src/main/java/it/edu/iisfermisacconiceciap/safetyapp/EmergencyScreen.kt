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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.edu.iisfermisacconiceciap.safetyapp.Background.Companion.getSnoozeLeft
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
        var timeLeft by remember { mutableStateOf(getSnoozeLeft()) }
//        var timeLeft by remember { mutableStateOf(Background.getSnoozeLeft()) }

        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(Modifier.fillMaxSize(), bottomBar = {
                Row(horizontalArrangement = Arrangement.SpaceAround) {
                    Button(
                        { snoozeUntil = Instant.now().plusSeconds(60 * 5) },
                        Modifier.padding(20.dp)
                    ) {
                        Text("Ignora per 5 minuti")
                    }
                    if (timeLeft != null) Text(
                        "Puoi usare il dispositivo liberamente per $timeLeft",
                        Modifier.align(Alignment.CenterVertically)
                    )
                }
            }) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(it),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
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
                            timeLeft = getSnoozeLeft()
                        }
                    })
                }
            }
        }
    }
}
