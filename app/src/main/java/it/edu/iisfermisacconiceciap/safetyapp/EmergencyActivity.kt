package it.edu.iisfermisacconiceciap.safetyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class EmergencyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmergencyScreen()
        }
    }
}
