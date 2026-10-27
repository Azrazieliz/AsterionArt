package com.asterion.art.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import com.asterion.art.brain.BrainCatalog
import com.asterion.art.brain.BrainDownloadWorker
import com.asterion.art.brain.BrainModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Android-local Qwen3 planning model, entirely separate from image-QNN weights. */
@Composable
fun BrainModelSection() {
    val context=LocalContext.current
    val store=remember { BrainModelStore(context) }
    var installed by remember { mutableStateOf(store.installed()) }
    var selected by remember { mutableStateOf(store.selected()?.id) }
    var contextTokens by remember { mutableIntStateOf(store.contextTokens()) }
    var importId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Map<String,Int>>(emptyMap()) }
    val picker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri ->
        val chosen=importId
        if(uri!=null&&chosen!=null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                BrainDownloadWorker.enqueue(context,chosen,uri.toString())
            }.onFailure{message=it.message.orEmpty()}
        }
    }
    LaunchedEffect(Unit) {
        while(true){
            installed=store.installed()
            selected=store.selected()?.id
            progress=withContext(Dispatchers.IO) {
                runCatching {
                    WorkManager.getInstance(context)
                        .getWorkInfosByTag("asterion-brain-download").get()
                        .filter { !it.state.isFinished }
                        .associate {
                            val model=it.tags.firstOrNull{tag->tag.startsWith("qwen3_")}.orEmpty()
                            model to it.progress.getInt("percent",0)
                        }
                }.getOrDefault(emptyMap())
            }
            delay(1500)
        }
    }
    Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Text("ASTERION DESIGN BRAIN",color=Gold,
                style=MaterialTheme.typography.titleMedium)
            Text("Local, real Qwen3 reasoning. It interprets concepts and methods before " +
                "any image or video model runs. The image model retains its own text limit.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
            Text("Context: "+contextTokens+" native LLM tokens",color=Gold)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                items(listOf(4096,8192,16384,32768)) { n ->
                    FilterChip(selected=contextTokens==n,onClick={
                        store.setContextTokens(n);contextTokens=n
                    },label={Text(if(n==32768)"32K" else (n/1024).toString()+"K")})
                }
            }
            Text("32K requires enough free RAM; choosing a larger context does not " +
                "increase the diffusion text encoder's capacity.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
            BrainCatalog.models.forEach { model ->
                val ready=installed.any { it.id==model.id }
                Text(model.label+(if(ready)" · Installed" else
                    " · "+(model.bytes/1048576)+" MiB"),
                    style=MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    if(ready) {
                        OutlinedButton(onClick={
                            store.select(model.id)
                            selected=model.id
                        }) {
                            Text(if(selected==model.id)"Active" else "Use model")
                        }
                    } else {
                        Button(onClick={
                            BrainDownloadWorker.enqueue(context,model.id)
                            message="Downloading "+model.label
                        }) { Text("Get from Hugging Face") }
                        OutlinedButton(onClick={
                            importId=model.id
                            picker.launch(arrayOf("application/octet-stream","*/*"))
                        }){ Text("Import GGUF") }
                    }
                }
                val value=progress[model.id]
                if(value!=null)LinearProgressIndicator(
                    progress={value/100f},modifier=Modifier.fillMaxWidth())
            }
            if(message.isNotBlank())Text(message,style=MaterialTheme.typography.bodySmall)
        }
    }
}
