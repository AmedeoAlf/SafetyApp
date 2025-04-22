package it.edu.iisfermisacconiceciap.safetyapp

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask

//val dataStore = preferencesDataStore(name = "connections_count") as DataStore<Preferences>

class Background : Service() {
    companion object {
        var notification: Notification? = null

        // Da eliminare probabilmente
        var total_connections = 0
        var total_unreachable = 0
    }

    private fun incrementPreferencesCounter(key: String) {
        CoroutineScope(Dispatchers.Default).launch {
            PreferencesManager(this@Background).incrementInt(
                key
            )
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun doRequest(url: URL, process: (input: String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
//            dataStore.edit { settings ->
//                settings[DATASTORE_KEY] = (settings[DATASTORE_KEY] ?: 0) + 1
//            }
            try {
                with(url.openConnection() as HttpURLConnection) {
                    process(inputStream.reader().readLines().joinToString("\n"))
                    disconnect()
                }
                total_connections += 1
                incrementPreferencesCounter("total_connections")
            } catch (_: ConnectException) {
                total_unreachable += 1
                incrementPreferencesCounter("total_unreachable")
            } catch (e: FileNotFoundException) {
                println("Couldn't find endpoint on server ($e)")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("Reached onStartCommand ${intent?.data}")
        incrementPreferencesCounter("onStartCommand")
        return START_STICKY
    }

    override fun onCreate() {
        val wakeLock: PowerManager.WakeLock =
            (getSystemService(POWER_SERVICE) as PowerManager).run {
                newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Background::Lock"
                ).apply { acquire(10 * 60 * 1000L * 100 /*1000 minutes*/) }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                println("MISSING PERMS???? " + checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
                return super.onCreate()
            }
        }
        println("We starting foreground")
        // Cheap solution
        notification = NotificationCompat.Builder(this, "overlay")
            .setContentTitle("Disabilita questa notifica")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentText("Clicca per andare nelle impostazioni (TODO)").build()
        NotificationManagerCompat.from(this).notify(1, notification!!)

        startForeground(
            2,
            notification,
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

        val updateFunc = {
            wakeLock.acquire(1000 * 60 * 1000L /*1000 minutes*/)
            doRequest(URL("http://192.168.178.22:3500/a")) { input ->
                try {
                    val response = JSONTokener(input).nextValue() as JSONObject
                    if (!response.getBoolean("emergency")) return@doRequest
                    startActivity(
                        Intent(
                            this@Background, EmergencyPopup::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: JSONException) {
                    println("Error in response contents $e")
                }

            }
        }

        Timer().schedule(
            object : TimerTask() {
                override fun run() = updateFunc()
            },
            0,
            2000
        )
        /*
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                updateFunc()
                handler.postDelayed(this, 2000)
            }
        })
        */

        return super.onCreate()
    }

}
