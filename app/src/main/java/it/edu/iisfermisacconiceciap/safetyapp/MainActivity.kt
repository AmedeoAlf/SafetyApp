package it.edu.iisfermisacconiceciap.safetyapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.SafetyAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.Date

class MainActivity : ComponentActivity() {
    private fun success() {

        startForegroundService(
            Intent(
                this, Background::class.java
            )
        )
//                startService(intent)
        setContent {
            SuccessScreen(PreferencesManager(this))
        }
    }

    @SuppressLint("BatteryLife")
    val permissionCards = listOf<PermissionCardWrapper>(
        PermissionCardWrapper(
            { !Settings.canDrawOverlays(this) }, PermissionCard(
                "Abilita SafetyApp in \"Mostra sopra altre app\" per mostrare l'allarme",
                "Apri impostazioni"
            ) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:$packageName".toUri()
                    )
                )
            }
        ),
        PermissionCardWrapper(
            {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            }, PermissionCard(
                "L'app non ha accesso alle notifiche", "Concedi accesso"
            ) {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                        Settings.EXTRA_APP_PACKAGE, packageName
                    )
                )
            }
        ),
        PermissionCardWrapper(
            {
                !getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(
                    packageName
                )
            }, PermissionCard(
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
    )

    private fun error(cards: List<PermissionCard>) {
        setContent {
            MissingPermScreen(cards)
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        // Esiste già?
        if (notificationManager.notificationChannels.find { chan -> chan.id == "overlay" } != null) return
        val channel = NotificationChannel(
            "overlay", "Disabilita notifiche", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Queste notifiche servono soltanto a creare il foregroundService"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun refreshMenu() {
        println("                   ISSUED REFRESH")

        val missingPerms =
            permissionCards.asSequence().filter { it.evalCondition() }.map { it.card }.toList()

        if (missingPerms.isEmpty()) success() else error(missingPerms)
    }

    override fun onResume() {
        super.onResume()
        refreshMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        // Chiedi i permessi per le notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) refreshMenu()
            }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Preview(group = "ok", device = "id:tv_4k")
@Preview(group = "ok", device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SuccessScreen(preferencesManager: PreferencesManager? = null) {
    var totalSuccessful by remember { mutableStateOf("---") }
    var totalUnreachable by remember { mutableStateOf("---") }
    var onStartCommand by remember { mutableStateOf("---") }
    var snoozeLeft by remember { mutableStateOf(Background.getSnoozeLeft()) }
    var lastReset by remember { mutableStateOf("---") }
    SafetyAppTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (snoozeLeft != null) Button({
                    Background.snoozeUntil = Instant.now().plusSeconds(60 * 5)
                }) {
                    Text("Allarme inibito per $snoozeLeft")
                }
                Text("OK", style = MaterialTheme.typography.displayLarge)
                Text("Tutti i permessi in regola")
                Text("Connessioni avvenute con successo: $totalSuccessful")
                Text("Connessioni fallite: $totalUnreachable")
                Text("onStartCommand: $onStartCommand")
                LaunchedEffect(lastReset) {
                    while(true) {
                        totalSuccessful = preferencesManager?.getInt("total_connections").toString()
                        totalUnreachable = preferencesManager?.getInt("total_unreachable").toString()
                        onStartCommand = preferencesManager?.getInt("onStartCommand").toString()
                        val lastResetTimestamp = preferencesManager?.getInstant("lastReset")
                        lastReset =
                            if (lastResetTimestamp == null) "mai" else Date.from(lastResetTimestamp)
                                .toString()
                        snoozeLeft = Background.getSnoozeLeft()
                        delay(100)
                    }
                }
                Button({
                    val now = Instant.now()
                    runBlocking {
                        preferencesManager?.setInt("total_connections", 0)
                        preferencesManager?.setInt("total_unreachable", 0)
                        preferencesManager?.setInt("onStartCommand", 0)
                        preferencesManager?.setInstant(
                            "lastReset", now
                        )
                    }
                    lastReset = now.toString()
                }) { Text("Reset statistiche") }
                Text("Last reset: $lastReset")
            }
        }
    }
}

data class PermissionCardWrapper(
    val evalCondition: () -> Boolean, val card: PermissionCard
)

data class PermissionCard(
    val description: String, val btnLabel: String, val action: () -> Unit
)

@Composable
fun PermissionCard(card: PermissionCard) {
    Card(Modifier.padding(6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(card.description, Modifier.fillMaxWidth())
            Button(card.action, Modifier.align(Alignment.End)) { Text(card.btnLabel) }
        }
    }
}

@Preview(group = "missing", device = "id:tv_4k")
@Preview(group = "missing", device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MissingPermScreen(
    cards: List<PermissionCard> = listOf(
        PermissionCard(
            "Permesso mancante", "Correggi"
        ) {},
        PermissionCard(
            "Permesso mancante molto più lungo vediamo come appare", "Apri impostazioni"
        ) {},
    )
) {
    SafetyAppTheme {
        Surface(Modifier.fillMaxSize()) {
            Column {
                Text(
                    "Permessi mancanti",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 30.dp)
                )
                LazyVerticalGrid(
                    GridCells.Adaptive(300.dp), contentPadding = PaddingValues(10.dp)
                ) {
                    items(cards) { card ->
                        PermissionCard(card)
                    }
                }
            }
        }
    }
}