package it.edu.iisfermisacconiceciap.safetyapp

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Duration
import java.time.Instant
import java.util.Locale

class FetchEmergencyService : Service() {
    companion object {
        val running = Mutex()

        var lastResponse: EmergencyState? by mutableStateOf(null)
            private set

        var snoozeUntil: Instant by mutableStateOf(Instant.now())

        // Stringa contente i secondi rimanenti di allarme disattivato, null => allarme in funzione
        fun getSnoozeLeft(): String? {
            if (snoozeUntil.isBefore(Instant.now())) return null

            val secsLeft = Duration.between(
                Instant.now(), snoozeUntil
            ).toMillis() / 1000

            return String.format(
                Locale.US, "%d:%02d", secsLeft.floorDiv(
                    60
                ), secsLeft % 60
            )
        }
    }

    private val util = Util(this)
    private lateinit var wakeLock: WakeLock

    // Funzione da eseguire ad intervallo regolare (*/2s)
    fun fetchAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
            try {
                val response = util.getJSONObject("requestSchoolStateJs.php")

                lastResponse = (lastResponse ?: EmergencyState.STARTING_STATE).updateWith(response)

                // Forza la schermata di allarme se c'è un'emergenza e non ci sono errori dal server
                // MA, in caso di errori, non forzare la schermata
                if (lastResponse!!.isEmergency && lastResponse!!.error == null && Instant.now()
                        .isAfter(snoozeUntil)
                ) startActivity(
                    Intent(
                        this@FetchEmergencyService, EmergencyActivity::class.java
                    ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                )
                util.dispatchIncrement("total_connections")
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException, is ConnectException -> util.dispatchIncrement("total_unreachable")
                }
                lastResponse = (lastResponse ?: EmergencyState.STARTING_STATE).updateWith(e)
            }
        }.invokeOnCompletion {
            Handler(Looper.getMainLooper()).postDelayed({ fetchAsync() }, 2000)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    // Non so cosa fa questa funzione
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("Reached onStartCommand ${intent?.data}")
        util.dispatchIncrement("onStartCommand")
        return START_STICKY
    }

    override fun onCreate() {
        // Solo un'istanza del servizio deve essere avviata
        if (!running.tryLock(this)) {
            stopSelf()
            return
        }

        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "Background::Lock"
        )

        // Non fare nulla in mancanza di accesso alle notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            println("MISSING PERMS???? " + checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
            return super.onCreate()
        }

        // Crea la notifica per il foreground service
        val notification = NotificationCompat.Builder(this, "overlay")
            .setContentTitle("Disabilita questa notifica").setSmallIcon(R.drawable.warning)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 1, Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        packageName,
                    ).putExtra(Settings.EXTRA_CHANNEL_ID, "overlay"), PendingIntent.FLAG_IMMUTABLE
                )
            ).setContentText("Tocca per andare nelle impostazioni").build()

        startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        println("Started foreground service")

        // Esegue il primo fetch (ogni fetch aggiunge in coda il prossimo)
        fetchAsync()

        return super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        running.unlock(this)
    }
}
