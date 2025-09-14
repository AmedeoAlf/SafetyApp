package it.edu.iisfermisacconiceciap.safetyapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri


class MainActivity : ComponentActivity() {
    private fun refreshMenu() {
        println("                   ISSUED REFRESH")
        val missingPerms = permissionCards.filter { it.shouldTrigger() }
        if (missingPerms.isEmpty()) showSuccessScreen() else showErrorScreen(missingPerms)
    }

    override fun onResume() {
        super.onResume()
        refreshMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        // Chiedi i permessi per le notifiche
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) refreshMenu()
            }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
    }

    private fun showSuccessScreen() {
        startForegroundService(Intent(this, FetchEmergencyService::class.java))
        setContent { SuccessScreen(PreferencesManager(this)) }
    }

    private fun showErrorScreen(cards: List<PermissionCard>) =
        setContent { MissingPermScreen(cards) }

    @SuppressLint("BatteryLife")
    val permissionCards = listOf(
        PermissionCard(
            { !Settings.canDrawOverlays(this) },
            "Abilita SafetyApp in \"Mostra sopra altre app\" per mostrare l'allarme",
            "Apri impostazioni"
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
            )
        },
        PermissionCard(
            {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            },
            "L'app non ha accesso alle notifiche", "Concedi accesso"
        ) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                    Settings.EXTRA_APP_PACKAGE, packageName
                )
            )
        },
        PermissionCard(
            {
                !getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(
                    packageName
                )
            },
            "Il servizio di emergenza è ristretto in background", "Disattiva restrizioni"
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    "package:$packageName".toUri()
                )
            )
        }

    )

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        // Esiste già il notification channel?
        if (notificationManager.notificationChannels.find { chan -> chan.id == "overlay" } != null) return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                "overlay", "Disabilitami", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Queste notifiche servono soltanto a creare il foregroundService"
            })
    }
}
