package it.edu.iisfermisacconiceciap.safetyapp

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket


class Background : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val responseBuf = ByteArray(100)
    private val serverAddr = InetAddress.getByName("192.168.178.22")
    fun request(): Int {
        var socket: Socket? = null
        try {
            socket = Socket(serverAddr, 9001)
            val input = socket.getInputStream()
            println("Waiting response")
            val bytesRead = input.read(responseBuf)
            println("Recieved " + responseBuf.slice(IntRange(0, bytesRead)))
            input.close()
            return if (bytesRead >= 1) responseBuf[0].toInt() else -1
        } catch (e: ConnectException) {
            println("Couldn't connect: $e")
        } finally {
            socket?.close()
        }
        return -1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                println("MISSING PERMS???? " + checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
                return super.onStartCommand(intent, flags, startId)
            }
        }
        println("We starting foreground")
        // Cheap solution
        val notification = NotificationCompat.Builder(this, "overlay")
            .setContentTitle("Disabilita questa notifica")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentText("Clicca per andare nelle impostazioni (TODO)").build()
        startForeground(
            2,
            notification,
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        NotificationManagerCompat.from(this).notify(1, notification)
        val handler = Handler(Looper.getMainLooper())
        val checkServer = object : Runnable {
            override fun run() {
                val result = request()
                if (result == 1) {
                    val popup =
                        Intent(
                            this@Background,
                            EmergencyPopup::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(popup)
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(checkServer)

        return super.onStartCommand(intent, flags, startId)
    }

}
