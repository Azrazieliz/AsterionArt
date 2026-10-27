package com.asterion.art.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.asterion.art.core.CoreCharacterSheet
import com.asterion.art.core.CoreSheetBridge
import com.asterion.art.data.ArtRepository
import com.asterion.art.model.SheetRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Core sheet selection uses searching only; no trait descriptions are authored. */
@Composable
fun CoreSheetBrowser(repo: ArtRepository, onQueued: () -> Unit) {
    val context = LocalContext.current
    val bridge = remember { CoreSheetBridge(context) }
    val scope=rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CoreCharacterSheet>>(emptyList()) }
    var failure by remember { mutableStateOf<String?>(null) }
    var selecting by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        delay(250L)
        runCatching { bridge.search(query) }
            .onSuccess { results = it; failure = null }
            .onFailure { results = emptyList(); failure = it.message }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("ASTERION CORE", color = Gold, style = MaterialTheme.typography.labelLarge)
            Text("Character sheets", style = MaterialTheme.typography.headlineMedium)
            Text("Search the authoritative Core library. Tap a sheet to import its full reference into Art.",
                color = Mist)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search Core sheets") },
                singleLine = true
            )
        }
        if (failure != null) item {
            Text(failure.orEmpty(), color = Red)
        }
        if (results.isEmpty() && failure == null) item {
            Text("No matching finished character sheets in Core.", color = Mist)
        }
        items(results, key = { it.sheetId }) { sheet ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(model = sheet.uri, contentDescription = sheet.characterName,
                        modifier = Modifier.size(80.dp, 112.dp),
                        contentScale = ContentScale.Fit)
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(sheet.characterName, color = White)
                        Text(sheet.seriesCode + " · " + sheet.characterId, color = Mist)
                        Button(
                            enabled = selecting==null,
                            onClick = {
                                selecting = sheet.sheetId
                                scope.launch {
                                    runCatching {
                                        bridge.verifySource(sheet)
                                        repo.importCoreCharacter(
                                            sheet.characterId,sheet.sheetId,
                                            sheet.characterName,sheet.uri.toString(),sheet.sha256)
                                    }.onSuccess {
                                        selecting=null
                                        onQueued()
                                    }.onFailure {
                                        selecting=null
                                        failure=it.message
                                    }
                                }
                            }
                        ) { Text("Select reference") }
                    }
                }
            }
        }
    }
}
