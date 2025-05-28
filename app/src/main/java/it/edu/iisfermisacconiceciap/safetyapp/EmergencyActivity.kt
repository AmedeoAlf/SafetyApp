package it.edu.iisfermisacconiceciap.safetyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class EmergencyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Background.isEmergency.observe(this) { if (!it) finish() }
        enableEdgeToEdge()
        setContent {
            EmergencyScreen()
        }
    }
}
