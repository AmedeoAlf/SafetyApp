package it.edu.iisfermisacconiceciap.safetyapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        val intent = Intent(this, Background::class.java)
        startForegroundService(intent)
//                startService(intent)
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }

    private fun error() {
        setContent {
            MissingPermScreen()
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            "overlay",
            "Disabilitami",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Queste notifiche servono soltanto a creare il foregroundService"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.setData("package:$packageName".toUri())
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        requestOverlayPermission()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) success() else error()
            }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            success()
        }
    }
}

@Preview(group = "ok", device = "id:tv_4k")
@Preview(group = "ok", device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreen() {
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

@Composable
fun PermissionCard(card: CardData) {
    Card(Modifier.padding(6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(card.description, Modifier.fillMaxWidth())
            Button(card.action, Modifier.align(Alignment.End)) { Text(card.btnLabel) }
        }
    }
}

data class CardData(val description: String, val btnLabel: String, val action: () -> Unit)

@Preview(group = "missing", device = "id:tv_4k")
@Preview(group = "missing", device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MissingPermScreen(
    cards: List<CardData> = listOf(
        CardData(
            "Permesso mancante",
            "Correggi"
        ) {},
        CardData(
            "Permesso mancante molto più lungo vediamo come appare",
            "Apri impostazioni"
        ) {},
    )
) {
    SafetyAppTheme {
        Surface(Modifier.fillMaxSize()) {
            LazyVerticalGrid(GridCells.Adaptive(250.dp), contentPadding = PaddingValues(10.dp)) {
                items(cards) { card ->
                    PermissionCard(card)
                }
            }
        }
    }
}