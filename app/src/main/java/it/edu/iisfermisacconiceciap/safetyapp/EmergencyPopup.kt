package it.edu.iisfermisacconiceciap.safetyapp

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.PreferencesManager
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.SafetyAppTheme

class EmergencyPopup : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmergencyScreen()
        }
    }

    @Preview(device = "id:tv_4k")
    @Preview(device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun EmergencyScreen() {
        // Sarebbe da implementare questo numero reattivamente
//    var flow by remember { mutableStateOf(Background.exampleCounterFlow) }
        var total_connections by remember { mutableStateOf("---") }
        var total_unreachable by remember { mutableStateOf("---") }
        SafetyAppTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "C'è stata un'emergenza",
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center
                    )
                    Text("Connessioni a buon fine: ${Background.total_connections}")
                    Text("Connessioni non riuscite: ${Background.total_unreachable}")
                }
                LaunchedEffect(Unit) {
                    with(PreferencesManager(this@EmergencyPopup)) {
                        total_unreachable = getInt("total_unreachable").toString()
                        total_connections = getInt("total_connections").toString()
                    }
                }
            }
        }
    }
}