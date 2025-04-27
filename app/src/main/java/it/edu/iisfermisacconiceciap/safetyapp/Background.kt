package it.edu.iisfermisacconiceciap.safetyapp

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import java.net.URL
import java.util.Timer
import java.util.TimerTask

//val dataStore = preferencesDataStore(name = "connections_count") as DataStore<Preferences>

class Background : Service() {
    companion object {
        var notification: Notification? = null
    }

    val util = Util(this)
    lateinit var wakeLock: PowerManager.WakeLock

    fun update() {
        wakeLock.acquire(1000 * 60 * 1000L /*1000 minutes*/)
        util.doRequest(URL("http://192.168.178.22:3500/a")) { response ->
            if (!response.getBoolean("emergency")) return@doRequest
            startActivity(
                Intent(
                    this@Background, EmergencyPopup::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("Reached onStartCommand ${intent?.data}")
        util.incrementPreferencesCounter("onStartCommand")
        return START_STICKY
    }

    override fun onCreate() {
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "Background::Lock"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                println("MISSING PERMS???? " + checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
                return super.onCreate()
            }
        }
        val notifIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(
            Settings.EXTRA_APP_PACKAGE,
            packageName,
        ).putExtra(
            Settings.EXTRA_CHANNEL_ID, "overlay"
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 1, notifIntent, PendingIntent.FLAG_IMMUTABLE
        )

        notification = NotificationCompat.Builder(this, "overlay")
            .setContentTitle("Disabilita questa notifica")
            .setSmallIcon(R.drawable.ic_launcher_background).setContentIntent(pendingIntent)
            .setContentText("Tocca per andare nelle impostazioni").build()

        startForeground(
            2,
            notification,
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        println("Started foreground service")

        Timer().schedule(
            object : TimerTask() {
                override fun run() = update()
            }, 0, 2000
        )/*
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                update()
                handler.postDelayed(this, 2000)
            }
        })
        */

        return super.onCreate()
    }

}
