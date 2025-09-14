package it.edu.iisfermisacconiceciap.safetyapp

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

data class ResponseData(
    val isEmergency: Boolean,
    val currEmergency: String,
    val currDescrizione: String,
    val serverError: String?,
) {
    fun updateWith(json: JSONObject): ResponseData {
        val stato = json.getIntOrNull("STATO")
        return ResponseData(
            isEmergency = if (stato == null) this.isEmergency else stato != 0,
            serverError = json.getStringOrNull("ERROR"),
            currEmergency = json.getStringOrNull("MESSAGGIO") ?: this.currEmergency,
            currDescrizione = json.getStringOrNull("DESCRIZIONE") ?: this.currDescrizione
        )
    }
}

class FetchEmergencyService : Service() {
    companion object {
        var running: FetchEmergencyService? = null

        val lastResponse = MutableLiveData<ResponseData>(
            ResponseData(
                isEmergency = false,
                currEmergency = "Nessuna emergenza",
                currDescrizione = "Nessuna descrizione",
                serverError = null
            )
        )

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
    fun fetch() {
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        util.dispatchRequest("requestSchoolStateJs.php") { response ->
            val parsed = lastResponse.value!!.updateWith(response)
            lastResponse.postValue(parsed)

            if (parsed.isEmergency && Instant.now().isAfter(snoozeUntil)) startActivity(
                Intent(
                    this@FetchEmergencyService, EmergencyActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
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
        // Solo un'istanza del servizio deve essere avviata
        if (running != null) return
        running = this

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

        // Chiama la funzione update ogni 2s per tutta la vita del servizio
        Timer().scheduleAtFixedRate(0L, 2000L) { fetch() }

        return super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (running == this) running = null
    }
}
