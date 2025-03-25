package it.edu.iisfermisacconiceciap.safetyapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.SafetyAppTheme

class MainActivity : ComponentActivity() {
    private fun success() {
        startForegroundService(
            Intent(
                this, Background::class.java
            )
        )
//                startService(intent)
        setContent {
            SuccessScreen()
        }
    }

    private fun error(cards: List<CardData>) {
        setContent {
            MissingPermScreen(cards)
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        // Esiste già?
        if (notificationManager.notificationChannels.find { chan -> chan.id == "overlay" } != null) return
        val channel = NotificationChannel(
            "overlay", "Disabilitami", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Queste notifiche servono soltanto a creare il foregroundService"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun refreshMenu() {
        println("                   ISSUED REFRESH")
        val errorCards = mutableListOf<CardData>()
        if (!Settings.canDrawOverlays(this)) errorCards.add(
            CardData(
                "Abilità SafetyApp in \"Mostra sopra altre app\" per mostrare l'allarme",
                "Apri impostazioni"
            ) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData("package:$packageName".toUri()))
            })
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            errorCards.add(
                CardData(
                    "L'app non ha accesso alle notifiche", "Concedi accesso"
                ) {
                    startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                            Settings.EXTRA_APP_PACKAGE, packageName
                        )
                    )
                })
        }
        if (errorCards.isEmpty()) success() else error(errorCards)
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
        // Mostra l'interfaccia (errori oppure "schermata OK")
        refreshMenu()
    }
}

@Preview(group = "ok", device = "id:tv_4k")
@Preview(group = "ok", device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SuccessScreen() {
    SafetyAppTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("OK", style = MaterialTheme.typography.displayLarge)
                Text("Tutti i permessi in regola")
            }
        }
    }
}

data class CardData(val description: String, val btnLabel: String, val action: () -> Unit)

@Composable
fun PermissionCard(card: CardData) {
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
    cards: List<CardData> = listOf(
        CardData(
            "Permesso mancante", "Correggi"
        ) {},
        CardData(
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