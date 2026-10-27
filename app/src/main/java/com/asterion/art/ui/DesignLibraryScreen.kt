package com.asterion.art.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.learning.CreativeMemoryStore
import com.asterion.art.learning.DesignReferenceSelection
import com.asterion.art.learning.SavedDesign
import com.asterion.art.model.RenderMode
import com.asterion.art.model.TaskKind
import com.asterion.art.work.GenerationWorker
import com.asterion.art.video.VideoPlanStore
import com.asterion.art.work.VideoGenerationWorker

/** Select, alter, regenerate or fuse designs; selection never grades or trains. */
@Composable
fun DesignLibraryScreen(repo:ArtRepository) {
    val context=LocalContext.current
    val memory=remember { CreativeMemoryStore(context) }
    var revision by remember { mutableIntStateOf(0) }
    val records=remember(revision) { memory.list().sortedByDescending { it.createdAt } }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var instructions by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<SavedDesign?>(null) }
    val marked=records.filter { it.id in selected }

    fun refresh() { revision++ }
    fun queue(design:SavedDesign) {
        runCatching {
            if(design.mode==RenderMode.ANIMATION) {
                val old=VideoPlanStore(context).load(design.jobId)
                val plan=VideoPlanStore(context).create(
                    old.imageUris,old.preset,old.secondsPerSegment,
                    design.prompt)
                val task=repo.recordTask(plan.id,TaskKind.VIDEO)
                VideoGenerationWorker.enqueue(context,plan.id,task.id)
            } else {
                val origin=runCatching {
                    GenerationJobStore(context).load(design.jobId)
                }.getOrNull()
                val originalSettings=origin?.settings ?: repo.settings
                val selectedRatio=if(design.creationMode==null ||
                    design.creationMode==com.asterion.art.model.CreationMode.STYLE_CORRECTION)
                    originalSettings.ratio else "9:16"
                val settings=originalSettings.copy(ratio=selectedRatio,batch=1)
                val pixels=DesignReferenceSelection.images(design,memory.list())
                val job=GenerationJobStore(context).create(
                    if(design.mode==RenderMode.ANIMATION) RenderMode.ANIME else design.mode,
                    design.sourceSheetIds,settings,design.creationMode,
                    origin?.sourceImageUri,origin?.concepts.orEmpty(),
                    design.prompt,design.negative,design.parents.ifEmpty {
                        listOf(design.id)
                    },designReferenceUris=pixels)
                val task=repo.recordTask(job.id,TaskKind.STILL_IMAGE)
                GenerationWorker.enqueue(context,job.id,task.id)
            }
        }.onSuccess {
            message="Single-design generation queued. Progress is on Home."
            pending=null
        }.onFailure { error ->
            message=error.message ?: "Could not queue this design"
            pending=null
        }
    }

    if(pending!=null) AlertDialog(
        onDismissRequest={pending=null},
        title={Text("Review selected design")},
        text={
            Column(Modifier.heightIn(max=400.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Positive prompt")
                Text(requireNotNull(pending).prompt,
                    maxLines=12,style=MaterialTheme.typography.bodySmall)
                Text("Negative prompt")
                Text(requireNotNull(pending).negative,
                    maxLines=5,style=MaterialTheme.typography.bodySmall)
                Text("The selected design's saved image pixels are carried into native image conditioning. Originals are unchanged; multi-design fusion requires a reference-capable installed model.",
                    style=MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton={TextButton(onClick={pending?.let(::queue)}) { Text("Render design") }},
        dismissButton={TextButton(onClick={pending=null}) { Text("Cancel") }}
    )

    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            Text("Design Library",style=MaterialTheme.typography.headlineMedium)
            Text("Select from any sheet, request an alteration, " +
                "or fuse two or more. Originals remain untouched.",
                style=MaterialTheme.typography.bodySmall)
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={refresh()}) { Text("Refresh") }
                OutlinedButton(onClick={selected=emptySet()}) { Text("Clear selection") }
            }
            message?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
        }
        if(records.isEmpty()) item {
            Text("Designs appear here after a completed native generation. Existing generations from earlier app versions remain in Creations.")
        }
        items(records,key={it.id}) { item ->
            val active=item.id in selected
            OutlinedCard(onClick={
                selected=if(active) selected-item.id else selected+item.id
            },modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    (item.previewUri ?: item.sheetUri)?.let { uri ->
                        AsyncImage(model=Uri.parse(uri),contentDescription="Source sheet",
                            contentScale=ContentScale.Fit,modifier=Modifier.size(100.dp,105.dp))
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("Design " + item.ordinal.toString().padStart(2,'0') +
                            " · Sheet " + item.sheetNumber,
                            style=MaterialTheme.typography.titleMedium)
                        Text(item.creationMode?.display ?: item.mode.display,
                            style=MaterialTheme.typography.labelSmall)
                        Text(item.status,style=MaterialTheme.typography.labelSmall)
                        Text(item.prompt.take(145),maxLines=3,
                            style=MaterialTheme.typography.bodySmall)
                        Text(if(active)"Selected" else "Tap to select",
                            style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if(marked.isNotEmpty()) {
            item {
                Text(marked.size.toString()+" design(s) selected")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(marked.size==1) {
                        OutlinedButton(onClick={pending=marked.single()}) {
                            Text("Regenerate")
                        }
                    }
                }
                OutlinedTextField(value=instructions,onValueChange={instructions=it},
                    label={Text(if(marked.size>1)"Fusion intent (optional)"
                        else "What should change?")},
                    modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=5)
                if(marked.size==1) Button(
                    enabled=instructions.isNotBlank(),
                    onClick={
                        runCatching {
                            val revised=memory.alter(marked.single().id,instructions)
                            selected=setOf(revised.id);instructions="";refresh()
                            pending=revised
                        }.onFailure { message=it.message }
                    }) { Text("Alter selected design") }
                if(marked.size>=2) Button(onClick={
                    runCatching {
                        val fused=memory.fuse(marked.map { it.id },instructions)
                        selected=setOf(fused.id);instructions="";refresh()
                        pending=fused
                    }.onFailure { message=it.message }
                }) { Text("Fuse across designs / sheets") }
            }
        }
    }
}
