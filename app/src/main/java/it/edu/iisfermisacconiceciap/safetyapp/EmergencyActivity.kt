package it.edu.iisfermisacconiceciap.safetyapp

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class EmergencyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // lastResponse should never be null with the service running
            FetchEmergencyService.lastResponse?.let { if (!it.isEmergency) finish() }
            EmergencyScreen()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            // Crash costanti di _System UI_ con la flag `WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED`
            // Sembra un bug di alcune build Oreo
            // https://stackoverflow.com/questions/47915026/android-alarm-clock-app-crashes-systemui-with-nullpointerexception-under-oreo-o
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON/* or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED*/)
        }
    }
}
