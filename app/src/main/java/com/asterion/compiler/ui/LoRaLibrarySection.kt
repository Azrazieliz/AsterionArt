package com.asterion.compiler.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.compiler.profiles.AndroidLoRaLibraryStore
import com.asterion.compiler.profiles.CheckpointProfile
import com.asterion.compiler.profiles.LoRaKeywordRouter
import kotlinx.coroutines.launch

/**
 * The browser, keyword matching and metadata are functional. NPU activation
 * intentionally requires an installed, verified premerged model variant.
 * It is never represented as dynamic tensor injection.
 */
@Composable
fun LoRaLibrarySection(checkpoint: CheckpointProfile, prompt: String) {
    val context = LocalContext.current
    val store = remember(context) { AndroidLoRaLibraryStore(context) }
    var assets by remember { mutableStateOf(store.list()) }
    var name by remember(checkpoint.id) { mutableStateOf("") }
    var triggers by remember(checkpoint.id) { mutableStateOf("") }
    var weight by remember(checkpoint.id) { mutableFloatStateOf(1.0f) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    store.import(
                        uri = uri,
                        name = name.ifBlank { uri.lastPathSegment ?: "Imported LoRA" },
                        triggerWords = triggers.split(',').map(String::trim).filter(String::isNotBlank),
                        checkpoint = checkpoint,
                        weight = weight,
                    )
                }.onSuccess {
                    assets = store.list()
                    message = "LoRA indexed. NPU activation still requires a compatible converted model variant."
                }.onFailure {
                    message = it.message ?: "Unable to import LoRA"
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("LoRA Library — ${checkpoint.displayName}")
        Text("Only matching keywords activate a LoRA request. Installed LoRA files are not loaded automatically.")
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("LoRA name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = triggers, onValueChange = { triggers = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Trigger keywords (comma separated)") },
            supportingText = { Text("Keyword matching is case-insensitive with whole-tag boundaries.") },
        )
        Text("Requested weight: ${"%.2f".format(weight)}")
        Slider(value = weight, onValueChange = { weight = it }, valueRange = 0f..2f)
        Button(
            onClick = { picker.launch(arrayOf("*/*")) },
            enabled = triggers.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Import LoRA from device") }
        message?.let { Text(it) }
        val relevant = assets.filter { checkpoint.id in it.allowedCheckpointIds }
        relevant.forEach { lora ->
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(lora.displayName)
                    Text("Triggers: ${lora.triggerWords.joinToString()} · weight ${lora.defaultWeight}")
                }
                OutlinedButton(onClick = {
                    store.remove(lora.id)
                    assets = store.list()
                    message = "LoRA entry removed from catalog. Original file preserved."
                }) { Text("Remove") }
            }
        }
        if (relevant.isNotEmpty()) {
            val plan = LoRaKeywordRouter.resolve(prompt, checkpoint, relevant, emptyList())
            if (plan.triggered.isNotEmpty()) {
                Text("Requested by prompt: ${plan.triggered.joinToString { it.displayName }}")
                plan.blockers.forEach { Text(it) }
            } else {
                Text("No configured LoRA keywords matched this prompt.")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
