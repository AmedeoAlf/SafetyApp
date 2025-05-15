package it.edu.iisfermisacconiceciap.safetyapp

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

//val dataStore = preferencesDataStore(name = "connections_count") as DataStore<Preferences>

class Background : Service() {
    companion object {
        var running: Background? = null
        var currEmergency = "Nessuna emergenza"
        var currDescrizione = "Nessuna descrizione"
        var isEmergency = false
        var snoozeUntil: Instant = Instant.now()

        // Stringa contente i secondi rimanenti di allarme disattivato, null => allarme in funzione
        fun getSnoozeLeft(): String? {
            // toMinutes() requires API LEVEL 31
            val secsLeft = Duration.between(
                Instant.now(), snoozeUntil
            ).toMillis() / 1000
            return if (secsLeft > 0) String.format(
                Locale.US, "%d:%02d", secsLeft.floorDiv(
                    60
                ), secsLeft % 60
            ) else null
        }
    }

    private val util = Util(this)
    private val wakeLock = getSystemService(PowerManager::class.java).newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK, "Background::Lock"
    )

    // Funzione da eseguire ad intervallo regolare (*/2s)
    fun update() {
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        util.doRequest("requestSchoolStateJs.php") { response ->
//            println(response.toString(0))
            isEmergency = response.getInt("STATO") != 0
            currEmergency = response.getString("MESSAGGIO")
            currDescrizione = response.getString("DESCRIZIONE")

            if (!isEmergency || Instant.now().isBefore(snoozeUntil)) return@doRequest
            startActivity(
                Intent(
                    this@Background,
                    EmergencyActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    // Non so cosa fa questa funzione
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("Reached onStartCommand ${intent?.data}")
        util.incrementPreferencesCounter("onStartCommand")
        return START_STICKY
    }

    override fun onCreate() {
        // È già in esecuzione un Background
        if (running != null) return
        running = this

        // Non fare nulla in mancanza di accesso alle notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            println("MISSING PERMS???? " + checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
            return super.onCreate()
        }

        // Crea la notifica per il foreground service
        val notification = NotificationCompat.Builder(this, "overlay")
            .setContentTitle("Disabilita questa notifica")
            .setSmallIcon(R.drawable.ic_launcher_background).setContentIntent(
                PendingIntent.getActivity(
                    this, 1, Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        packageName,
                    ).putExtra(Settings.EXTRA_CHANNEL_ID, "overlay"), PendingIntent.FLAG_IMMUTABLE
                )
            ).setContentText("Tocca per andare nelle impostazioni").build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(2, notification)
        }
        println("Started foreground service")

        // Chiama la funzione update ogni 2s per tutta la vita del processo
        Timer().schedule(
            object : TimerTask() {
                override fun run() = update()
            }, 0, 2000
        )
        // Implementazione alternativa
        /*
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

    override fun onDestroy() {
        super.onDestroy()
        if (running == this) running = null
    }
}
