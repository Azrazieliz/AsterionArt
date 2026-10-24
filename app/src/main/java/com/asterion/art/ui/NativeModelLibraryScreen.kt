package com.asterion.art.ui

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.LazyRow
import com.asterion.art.model.RenderMode
import com.asterion.art.nativeengine.ModelOutputRouting
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.art.nativeengine.EmbeddedNativeRunner
import com.asterion.art.nativeengine.NativeModelLibrary
import com.asterion.art.work.NativeModelImportWorker

@Composable
fun NativeModelLibraryScreen() {
    val context=LocalContext.current
    val library=remember(context) { NativeModelLibrary(context) }
    val routing=remember(context) { ModelOutputRouting(context) }
    var installed by remember { mutableStateOf(library.list()) }
    var tagRefresh by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedFamily by remember { mutableStateOf("auto") }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val name=context.contentResolver.query(uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
                    if(it.moveToFirst()) it.getString(0) else null
                }?.removeSuffix(".zip") ?: "Imported QNN checkpoint"
                NativeModelImportWorker.enqueue(context,uri.toString(),name,selectedFamily)
            }.onSuccess { id -> message="Model installation queued: "+id.take(12) }
                .onFailure { e -> message=e.message ?: "Could not read model archive" }
        }
    }
    LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp),
        contentPadding=androidx.compose.foundation.layout.PaddingValues(18.dp)) {
        item {
            Text("NATIVE MODEL LIBRARY",color=Gold,
                style=MaterialTheme.typography.labelLarge)
            Text("Phone-side QNN checkpoints",
                style=MaterialTheme.typography.headlineMedium)
            Text("Model archives are streamed directly into permanent Asterion Art storage. "+
                "An extra ZIP copy is not made. Native processing uses this app's own executable.",
                color=Mist)
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(if(EmbeddedNativeRunner.isEmbedded(context))
                        "Embedded native engine included"
                        else "Embedded native engine missing",
                        color=if(EmbeddedNativeRunner.isEmbedded(context))Green else Red)
                    Text("Imported models: "+installed.size,color=Mist)
                }
            }
        }
        item {
            Text("MODEL FAMILY · SELECT IF ZIP HAS NO MARKER",color=Gold)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                items(listOf("auto","anima","sdxl","sd15npu","zimage",
                    "klein","qwen21","krea2turbo")) { family ->
                    FilterChip(selected=selectedFamily==family,
                        onClick={selectedFamily=family},
                        label={Text(family.uppercase())})
                }
            }
            Text("Auto reads packaged QNN markers. A selected family is only applied " +
                "when the archive contains compiled NPU tensors but no marker. " +
                "Raw Illustrious .safetensors must be converted first.",
                style=MaterialTheme.typography.bodySmall,color=Mist)
        }
        item {
            Button(onClick={picker.launch(arrayOf("application/zip","application/octet-stream","*/*"))},
                modifier=Modifier.fillMaxWidth()) {
                Text("Import QNN model ZIP")
            }
        }
        item {
            OutlinedButton(onClick={installed=library.list()},modifier=Modifier.fillMaxWidth()) {
                Text("Refresh installed models")
            }
        }
        if(message != null) item { Text(message.orEmpty(),color=Gold) }
        if(installed.isEmpty()) item {
            Text("No QNN model installed in Asterion Art yet.",color=Mist)
        }
        items(installed,key={it.id}) { model ->
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text(model.label,style=MaterialTheme.typography.titleMedium)
                    Text(model.backend.uppercase()+" · "+
                        (model.sizeBytes / 1048576L)+" MiB",color=Mist)
                    Text("Content checksum "+model.contentSha256.take(18)+"…",
                        style=MaterialTheme.typography.bodySmall,color=Mist)
                    Text("OUTPUT TYPES",color=Gold,style=MaterialTheme.typography.labelSmall)
                    val chosen=remember(model.id,tagRefresh) { routing.tags(model.id) }
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        items(listOf(RenderMode.ANIME,RenderMode.REALISTIC,RenderMode.COSPLAY)) { type ->
                            FilterChip(selected=type in chosen,
                                onClick={
                                    routing.set(model.id,type,type !in chosen)
                                    tagRefresh++
                                },label={Text(type.display)})
                        }
                    }
                }
            }
        }
    }
}
