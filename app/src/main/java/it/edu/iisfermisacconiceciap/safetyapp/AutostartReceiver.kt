package it.edu.iisfermisacconiceciap.safetyapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutostartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        context.startForegroundService(Intent(context, FetchEmergencyService::class.java))
        println("SafetyApp avviata automaticamente")
    }
}