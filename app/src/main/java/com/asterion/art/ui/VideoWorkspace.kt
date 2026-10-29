package com.asterion.art.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.asterion.art.data.ArtRepository
import com.asterion.art.model.TaskKind
import com.asterion.art.video.MotionPreset
import com.asterion.art.video.WanVideoModelPack
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import com.asterion.art.video.VideoPlanStore
import com.asterion.art.video.VideoResultStore
import com.asterion.art.work.VideoGenerationWorker
import com.asterion.art.work.VideoJoinWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun VideoWorkspace(repo: ArtRepository, onQueued: () -> Unit) {
    val context=LocalContext.current
    val taxonomy=remember { com.asterion.art.engine.ConceptTaxonomy(context) }
    var modelReady by remember { mutableStateOf(WanVideoModelPack.ready(context)!=null) }
    var modelParts by remember { mutableStateOf(
        WanVideoModelPack.resolved(context).size) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(3000)
            modelParts=withContext(Dispatchers.IO) {
                WanVideoModelPack.resolved(context).size
            }
            modelReady=modelParts==WanVideoModelPack.components.size
        }
    }
    var diagnostic by remember { mutableStateOf<String?>(null) }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var preset by remember { mutableStateOf(MotionPreset.CINEMATIC) }
    var seconds by remember { mutableStateOf(2) }
    var repeatSegments by remember { mutableStateOf(1) }
    var customMotion by remember { mutableStateOf("") }
    var showPromptReview by remember { mutableStateOf(false) }
    var resultsNonce by remember { mutableStateOf(0) }
    var localClips by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope=rememberCoroutineScope()

    val picker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()) { chosen ->
        if(chosen.isNotEmpty()) {
            if(images.size+chosen.count { it.toString() !in images }>8) {
                diagnostic="At most eight ordered reference frames; no image removed."
            } else {
                val good=chosen.all {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            it,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }.isSuccess
                }
                if(good) images=(images+chosen.map { it.toString() }).distinct()
                else diagnostic="Could not retain all selected image permissions."
            }
        }
    }
    val clipsPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()){chosen->
        if(chosen.isNotEmpty()) {
            val approved=chosen.all{uri->
                runCatching{
                    context.contentResolver.takePersistableUriPermission(
                        uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }.isSuccess
            }
            if(approved) {
                localClips=(localClips+chosen.map{it.toString()}).distinct()
                diagnostic=null
            } else diagnostic="Cannot retain all chosen MP4 permissions."
        }
    }
    val results=remember(resultsNonce) { VideoResultStore(context).list() }
    val prompts=VideoPlanStore.compileMotion(preset,customMotion,taxonomy)
    if(showPromptReview) AlertDialog(
        onDismissRequest={showPromptReview=false},
        title={Text("Review motion instructions")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("POSITIVE");Text(prompts.first)
            Text("NEGATIVE GUIDANCE");Text(prompts.second)
            Text("Positive and negative motion constraints are sent to the local Wan diffusion model.")
        }},
        confirmButton={TextButton(onClick={
            showPromptReview=false
            runCatching {
                val plan=VideoPlanStore(context).create(images,preset,seconds,customMotion,
                    if(images.size==1)repeatSegments else images.size)
                val task=repo.recordTask(plan.id,TaskKind.VIDEO)
                VideoGenerationWorker.enqueue(context,plan.id,task.id)
            }.onSuccess { onQueued() }.onFailure { diagnostic=it.message }
        }){Text("Approve and queue")}},
        dismissButton={TextButton(onClick={showPromptReview=false}){Text("Back")}})
    LazyColumn(
        modifier=Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("ASTERION ART · VIDEO",color=Gold)
                Text("Animation Studio",
                    style=androidx.compose.material3.MaterialTheme.typography.headlineLarge)
                Text("Real, fully offline Wan 2.2 TI2V inference integrated in Asterion Art. Each source image conditions generated neural frames; multiple anchors form separate clips joined as an MP4. No cloud or manual phone-side server.",
                    color=Mist)
            }
        }
        item {
            Card(modifier=Modifier.fillMaxWidth().padding(horizontal=18.dp),
                colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("OFFLINE WAN 2.2 VIDEO MODEL",color=Gold)
                    Text(if(modelReady)
                        "Three verified neural components installed · ready for local inference"
                        else "Verified model components: $modelParts / 3",
                        color=if(modelReady)Green else Mist)
                    Text("On-device native video diffusion requires the "+
                        "Wan 2.2 TI2V 5B image/video model, its matching VAE and "+
                        "UMT5 encoder. These are substantial one-time downloads; "+
                        "existing verified Asterion Hugging Face files are reused. "+
                        "No server, API token, paid service or cloud inference.",
                        color=Mist,style=androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    if(!modelReady) OutlinedButton(onClick={
                        scope.launch {
                            diagnostic="Resolving pinned Hugging Face revisions and LFS checksums"
                            val result=withContext(Dispatchers.IO) {
                                runCatching{WanVideoModelPack.enqueueMissing(context)}
                            }
                            diagnostic=result.fold(
                                onSuccess={"Verified downloads queued: "+it.size+
                                    ". Model installation progress is available in Models and notifications."},
                                onFailure={it.message?:"Video model installation failed"})
                        }
                    },modifier=Modifier.fillMaxWidth()){
                        Text("Install missing offline neural video models")
                    }
                    OutlinedButton(onClick={
                        modelParts=WanVideoModelPack.resolved(context).size
                        modelReady=modelParts==3
                        diagnostic=if(modelReady)"All video components registered and verified"
                            else "Missing "+(3-modelParts)+" video model component(s)"
                    }){Text("Check model installation")} 
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("SOURCE KEYFRAMES",color=Gold)
                Text("Choose one image for image-to-video, or several for ordered segmented animation.",color=Mist)
                OutlinedButton(onClick={ picker.launch(arrayOf("image/*")) }) {
                    androidx.compose.material3.Icon(Icons.Default.AddPhotoAlternate,null)
                    Spacer(Modifier.width(7.dp))
                    Text("Choose images")
                }
            }
        }
        if(images.isNotEmpty()) {
            items(images.indices.toList()) { position ->
                val value=images[position]
                Card(modifier=Modifier.fillMaxWidth().padding(horizontal=18.dp),
                    colors=CardDefaults.cardColors(containerColor=Panel)) {
                    Row(Modifier.padding(9.dp),
                        horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        AsyncImage(model=Uri.parse(value),
                            contentDescription="Video keyframe "+(position+1),
                            modifier=Modifier.size(85.dp,110.dp),
                            contentScale=ContentScale.Fit)
                        Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("KEYFRAME " + (position+1),color=Gold)
                            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                OutlinedButton(enabled=position>0,onClick={
                                    val new=images.toMutableList()
                                    val swap=new[position-1]
                                    new[position-1]=new[position]
                                    new[position]=swap
                                    images=new
                                }) { Text("↑") }
                                OutlinedButton(enabled=position<images.lastIndex,onClick={
                                    val new=images.toMutableList()
                                    val swap=new[position+1]
                                    new[position+1]=new[position]
                                    new[position]=swap
                                    images=new
                                }) { Text("↓") }
                                OutlinedButton(onClick={
                                    images=images.filterIndexed { i,_->i!=position }
                                }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("MOTION TYPE",color=Gold)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(MotionPreset.entries.toList()) { item ->
                        FilterChip(selected=preset==item,
                            onClick={preset=item},label={Text(item.title)})
                    }
                }
                OutlinedTextField(value=customMotion,onValueChange={customMotion=it},
                    label={Text("Specific motion direction")},modifier=Modifier.fillMaxWidth(),
                    minLines=2,maxLines=5)
                Text("Segment duration: $seconds s",color=Gold)
                Slider(value=seconds.toFloat(),
                    onValueChange={seconds=it.roundToInt()},
                    valueRange=1f..5f,steps=3)
                if(images.size==1) {
                    Text("Consecutive segments: "+repeatSegments,color=Gold)
                    Slider(value=repeatSegments.toFloat(),
                        onValueChange={repeatSegments=it.roundToInt()},
                        valueRange=1f..8f,steps=6)
                    Text("After the first clip, each new clip starts from the " +
                        "last decoded frame of the preceding clip. This improves " +
                        "continuation but cannot guarantee seamless motion.",color=Mist)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                // Start after validation. Optional prompt review remains available.
                Button(
                    enabled=modelReady&&images.isNotEmpty(),
                    modifier=Modifier.fillMaxWidth(),
                    onClick={
                        runCatching {
                            val plan=VideoPlanStore(context).create(images,preset,seconds,
                                customMotion,if(images.size==1)repeatSegments else images.size)
                            val task=repo.recordTask(plan.id,TaskKind.VIDEO)
                            VideoGenerationWorker.enqueue(context,plan.id,task.id)
                        }.onSuccess { onQueued() }.onFailure { diagnostic=it.message }
                    }
                ) { Text("Start video in background") }
                OutlinedButton(
                    enabled=modelReady&&images.isNotEmpty(),
                    modifier=Modifier.fillMaxWidth(),
                    onClick={showPromptReview=true}
                ) {
                    androidx.compose.material3.Icon(Icons.Default.PlayArrow,null)
                    Spacer(Modifier.width(8.dp))
                    Text("Inspect video prompt (optional)")
                }
                Text("Generation uses the embedded llmedge Wan video runtime. "+
                    "A high-memory device is required; native model load or RAM errors "+
                    "are reported without claiming success. Background operation "+
                    "remains subject to Android system restrictions.",
                    color=Mist,style=androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
        if(diagnostic!=null) item {
            Text(diagnostic.orEmpty(),Modifier.padding(horizontal=18.dp),color=Mist)
        }
        item {
            Card(modifier=Modifier.fillMaxWidth().padding(horizontal=18.dp),
                colors=CardDefaults.cardColors(containerColor=Panel)){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("OFFLINE MP4 ASSEMBLY TEST",color=Gold)
                    Text("Join videos already on this phone without Nightmare, "+
                        "network access or AI inference. Compatible audio/video "+
                        "tracks are preserved; different codecs are reported.",
                        color=Mist)
                    OutlinedButton(onClick={clipsPicker.launch(arrayOf("video/mp4"))}){
                        Text("Choose existing MP4 clips")
                    }
                    localClips.forEachIndexed { index,uri ->
                        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                            Text("Clip "+(index+1),Modifier.weight(1f),color=Mist)
                            OutlinedButton(enabled=index>0,onClick={
                                val list=localClips.toMutableList()
                                val prior=list[index-1]
                                list[index-1]=list[index];list[index]=prior
                                localClips=list
                            }){Text("↑")}
                            OutlinedButton(enabled=index<localClips.lastIndex,onClick={
                                val list=localClips.toMutableList()
                                val following=list[index+1]
                                list[index+1]=list[index];list[index]=following
                                localClips=list
                            }){Text("↓")}
                            TextButton(onClick={
                                localClips=localClips.filterIndexed{i,_->i!=index}
                            }){Text("Remove")}
                        }
                    }
                    Button(enabled=localClips.size>=2,onClick={
                        diagnostic=runCatching{
                            val id=VideoJoinWorker.enqueue(context,localClips)
                            "MP4 assembly queued: "+id
                        }.getOrElse{it.message?:"Cannot queue local video"}
                        if(diagnostic?.startsWith("MP4 assembly queued")==true)onQueued()
                    }){Text("Join and export existing MP4 clips")}
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal=18.dp)) {
                Text("VIDEO RESULTS",color=Gold)
                OutlinedButton(onClick={resultsNonce++}) { Text("Refresh results") }
            }
        }
        items(results,key={it.jobId}) { result ->
            Card(modifier=Modifier.fillMaxWidth().padding(horizontal=18.dp),
                colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(14.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(result.engine,color=Gold)
                    Text(result.segmentCount.toString()+
                        if(result.engine.startsWith("Offline")) " joined clip(s)"
                        else " inferred segment(s)",color=Mist)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={
                            val intent=Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(result.outputUri),"video/mp4")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        }) { Text("Play") }
                    }
                }
            }
        }
    }
}
