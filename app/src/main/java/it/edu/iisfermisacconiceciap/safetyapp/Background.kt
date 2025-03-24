package it.edu.iisfermisacconiceciap.safetyapp

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

//val dataStore = preferencesDataStore(name = "connections_count") as DataStore<Preferences>

class Background : Service() {
    companion object {
        var notification: Notification? = null
        var total_connections = 0
        var total_unreachable = 0

//        val DATASTORE_KEY = intPreferencesKey("val")
//        val exampleCounterFlow: Flow<Int> = dataStore.data
//            .map { preferences ->
//                preferences[DATASTORE_KEY] ?: 0
//            }

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
            } catch (e: ConnectException) {
                total_unreachable += 1
                println(e.message)
            } catch (e: FileNotFoundException) {
                println("Couldn't find endpoint on server ($e)")
            }
        }
    }

    override fun onCreate() {

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
            .setContentText("Clicca per andare nelle impostazioni (TODO)")
            .build()
        NotificationManagerCompat.from(this).notify(1, notification!!)

        startForeground(
            2,
            notification,
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        val handler = Handler(Looper.getMainLooper())
        val checkServer = object : Runnable {
            override fun run() {
                doRequest(URL("http://192.168.178.22:3500/a")) { input ->
                    try {
                        val response = JSONTokener(input).nextValue() as JSONObject
                        if (response.getBoolean("emergency")) {
                            val popup =
                                Intent(
                                    this@Background,
                                    EmergencyPopup::class.java
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(popup)
                        }
                    } catch (e: JSONException) {
                        println("Error in response contents $e")
                    }

                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(checkServer)

        return super.onCreate()
    }

}
