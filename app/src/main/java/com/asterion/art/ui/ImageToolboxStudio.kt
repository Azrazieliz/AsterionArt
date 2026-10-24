package com.asterion.art.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.asterion.art.data.ArtRepository
import com.asterion.art.enhancement.ImageToolSettings
import com.asterion.art.model.GradedAsset
import com.asterion.art.model.TaskKind
import com.asterion.art.work.ImageToolboxWorker

/** Internal Image Toolbox: no third-party activity or app switching required. */
@Composable
fun ImageToolboxStudio(asset:GradedAsset?, repo:ArtRepository, onQueued:()->Unit) {
    val context=LocalContext.current
    var brightness by remember(asset?.uri) { mutableStateOf(0f) }
    var contrast by remember(asset?.uri) { mutableStateOf(1f) }
    var saturation by remember(asset?.uri) { mutableStateOf(1f) }
    var sharpen by remember(asset?.uri) { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Text("IMAGE TOOLBOX",color=Gold,
                style=MaterialTheme.typography.labelMedium)
            Text("Finish and refine",style=MaterialTheme.typography.headlineLarge)
            Text("Built-in pixel processing. Settings never alter source artwork and each result is saved separately.",
                color=Mist)
        }
        if(asset==null) item { Text("Select a result to edit.",color=Mist) }
        if(asset!=null) {
            item {
                AsyncImage(model=Uri.parse(asset.uri),
                    contentDescription="Selected source artwork",contentScale=ContentScale.Fit,
                    modifier=Modifier.fillMaxWidth().height(300.dp))
            }
            item {
                Text("BRIGHTNESS",color=Gold)
                Text((brightness.toInt()).toString(),color=Mist)
                Slider(value=brightness,onValueChange={brightness=it},
                    valueRange=-65f..65f)
            }
            item {
                Text("CONTRAST",color=Gold)
                Text("%.2f".format(contrast),color=Mist)
                Slider(value=contrast,onValueChange={contrast=it},
                    valueRange=0.5f..1.8f)
            }
            item {
                Text("SATURATION",color=Gold)
                Text("%.2f".format(saturation),color=Mist)
                Slider(value=saturation,onValueChange={saturation=it},
                    valueRange=0f..2f)
            }
            item {
                Text("SHARPNESS",color=Gold)
                Text("%.2f".format(sharpen),color=Mist)
                Slider(value=sharpen,onValueChange={sharpen=it},
                    valueRange=0f..1.75f)
            }
            item {
                OutlinedButton(onClick={
                    brightness=0f;contrast=1f;saturation=1f;sharpen=0f
                },modifier=Modifier.fillMaxWidth()) {Text("Reset adjustments")}
            }
            item {
                Button(modifier=Modifier.fillMaxWidth(),onClick={
                    runCatching {
                        val options=ImageToolSettings(brightness,contrast,saturation,sharpen)
                        val task=repo.recordTask(asset.uri,TaskKind.ENHANCEMENT)
                        ImageToolboxWorker.enqueue(context,task.id,asset.uri,options)
                    }.onSuccess{onQueued()}.onFailure {error=it.message}
                }){Text("Process and save in background")}
            }
        }
        if(error!=null)item {Text(error.orEmpty(),color=Red)}
    }
}
