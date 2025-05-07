package it.edu.iisfermisacconiceciap.safetyapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutostartReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        context.startForegroundService(Intent(context, Background::class.java))
        println("SafetyApp avviata automaticamente")
    }
}