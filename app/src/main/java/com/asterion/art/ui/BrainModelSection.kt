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
import com.asterion.art.modelpack.EmbeddedModelInstallWorker
import com.asterion.art.modelpack.EmbeddedModelPack
import com.asterion.art.brain.BrainModelStore
import com.asterion.art.brain.BrainAdapterLibrary
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Android-local Qwen3 planning model, entirely separate from image-QNN weights. */
@Composable
fun BrainModelSection() {
    val context=LocalContext.current
    val store=remember { BrainModelStore(context) }
    val adapters=remember { BrainAdapterLibrary(context) }
    val scope=rememberCoroutineScope()
    var modelAdapters by remember { mutableStateOf(adapters.all()) }
    var adapterBusy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(store.installed()) }
    var selected by remember { mutableStateOf(store.selected()?.id) }
    var contextTokens by remember { mutableIntStateOf(store.contextTokens()) }
    var message by remember { mutableStateOf("") }
    var bundledProgress by remember { mutableIntStateOf(0) }
    var bundleError by remember { mutableStateOf<String?>(null) }
    val packaged=remember{EmbeddedModelPack(context).packaged()}
    val adapterPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri ->
        val base=selected
        if(uri!=null && base!=null) {
            scope.launch {
                adapterBusy=true
                val result=withContext(Dispatchers.IO) {
                    runCatching {
                        adapters.import(uri,base)
                    }
                }
                adapterBusy=false
                result.onSuccess {
                    modelAdapters=adapters.all()
                    message="GGUF adapter verified and imported; enable to apply."
                }.onFailure { message=it.message.orEmpty() }
            }
        }
    }
    LaunchedEffect(Unit) {
        while(true){
            installed=store.installed()
            selected=store.selected()?.id
            val info=withContext(Dispatchers.IO) {
                runCatching{
                    WorkManager.getInstance(context)
                        .getWorkInfosByTag(EmbeddedModelInstallWorker.TAG).get()
                        .maxByOrNull{it.id.toString()}
                }.getOrNull()
            }
            bundledProgress=info?.progress?.getInt("percent",0)?:0
            bundleError=info?.takeIf{it.state==androidx.work.WorkInfo.State.FAILED}
                ?.outputData?.getString("error")
            delay(1500)
        }
    }
    Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Text("ASTERION DESIGN BRAIN",color=Gold,
                style=MaterialTheme.typography.titleMedium)
            Text("Qwen3 reasoning weights are included inside the full offline "+
                "Asterion Art package and prepared from local APK assets on first "+
                "launch. No model download or model-file import is required.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
            if(!packaged){
                Text("THIS BUILD HAS NO BUNDLED GGUF WEIGHTS. The full offline "+
                    "release package is required.",color=Red)
            }else if(!EmbeddedModelPack(context).allReady()){
                Text("Preparing included model files · "+bundledProgress+"%",
                    color=Gold)
                LinearProgressIndicator(
                    progress={bundledProgress/100f},
                    modifier=Modifier.fillMaxWidth())
                bundleError?.let{Text(it,color=Red)}
                OutlinedButton(onClick={
                    EmbeddedModelInstallWorker.enqueue(context)
                    message="Retrying local extraction from this APK. No network used."
                }){Text("Retry offline model preparation")}
            }
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
            BrainCatalog.models.filter{
                it.id=="qwen3_17_q4"||installed.any{ready->ready.id==it.id}
            }.forEach { model ->
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
                        Text("Included checkpoint awaiting offline extraction",
                            color=Mist,style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if(selected!=null) {
                HorizontalDivider()
                Text("QWEN3 LORA ADAPTERS",color=Gold)
                Text("Select an adapter compatible with the active Qwen3 base. " +
                    "Loading it modifies live neural inference; it does not " +
                    "retrain the diffusion model.",color=Mist)
                OutlinedButton(enabled=!adapterBusy,onClick={
                    adapterPicker.launch(arrayOf("*/*"))
                }) { Text(if(adapterBusy)"Importing adapter…" else "Import GGUF LoRA") }
                modelAdapters.forEach { adapter ->
                    Text(adapter.id.take(24)+" · "+adapter.baseId+
                        if(adapter.enabled)" · ACTIVE" else "",color=Mist)
                    if(adapter.baseId==selected && !adapter.enabled) {
                        OutlinedButton(onClick={
                            runCatching { adapters.enable(adapter.id) }
                                .onSuccess {
                                    modelAdapters=adapters.all()
                                    message="Adapter selected for next reasoning generation."
                                }.onFailure { message=it.message.orEmpty() }
                        }) { Text("Enable compatible adapter") }
                    }
                }
                if(modelAdapters.any{it.enabled}) {
                    OutlinedButton(onClick={
                        adapters.disableAll()
                        modelAdapters=adapters.all()
                    }) { Text("Use unchanged base model") }
                }
            }
            if(message.isNotBlank())Text(message,style=MaterialTheme.typography.bodySmall)
        }
    }
}
