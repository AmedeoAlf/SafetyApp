package it.edu.iisfermisacconiceciap.safetyapp

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import it.edu.iisfermisacconiceciap.safetyapp.ui.theme.SafetyAppTheme

@Composable
fun PermissionCard(card: PermissionCard) {
    Card(Modifier.padding(6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(card.description, Modifier.fillMaxWidth())
            Button(card.fix, Modifier.align(Alignment.End)) { Text(card.btnLabel) }
        }
    }
}

@Preview(group = "missing", device = "id:tv_4k")
@Preview(group = "missing", device = "id:pixel_6", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MissingPermScreen(
    cards: List<PermissionCard> = listOf(
        PermissionCard("Permesso mancante", "Correggi") {},
        PermissionCard(
            "Permesso mancante molto più lungo vediamo come appare questa scritta all'interno della card",
            "Apri impostazioni"
        ) {},
    )
) {
    SafetyAppTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(WindowInsets.safeDrawing.asPaddingValues())) {
                Text(
                    "Permessi mancanti",
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 30.dp),
                    style = MaterialTheme.typography.displayMedium,
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
