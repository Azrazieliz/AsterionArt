package com.asterion.art.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.learning.UserPreferenceStore
import com.asterion.art.methodology.MethodologyRegistry
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import com.asterion.art.model.SheetRole
import com.asterion.art.model.TaskKind
import com.asterion.art.work.GenerationWorker
import org.json.JSONObject

data class SelectableConcept(val id: String, val label: String)

/** The canonical 455-character-concept registry is bundled, never fetched at runtime. */
object ConceptCatalog {
    fun load(context: android.content.Context): List<SelectableConcept> =
        context.assets.open("methodology/concepts_v3.json").bufferedReader().use { reader ->
            val list=JSONObject(reader.readText()).getJSONArray("entries")
            (0 until list.length()).map { index ->
                val value=list.getJSONObject(index)
                SelectableConcept(value.getString("id"),value.getString("name"))
            }.sortedBy { it.label.lowercase() }
        }
}

@Composable
private fun DestinationCard(title: String, description: String,
                            icon: androidx.compose.ui.graphics.vector.ImageVector,
                            onClick: ()->Unit) {
    Card(
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick),
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=Raised)
    ) {
        Row(Modifier.padding(19.dp),horizontalArrangement=Arrangement.spacedBy(15.dp)) {
            androidx.compose.material3.Icon(icon,null,tint=Gold,modifier=Modifier.size(29.dp))
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(title,style=MaterialTheme.typography.titleLarge,color=White)
                Text(description,color=Mist,style=MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CreativeGateway(openImage:()->Unit,openAnimation:()->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text("CREATE",color=Gold,style=MaterialTheme.typography.labelMedium) }
        item { Text("What are you creating?",style=MaterialTheme.typography.headlineLarge) }
        item { DestinationCard("Image","Illustration, methodology defaults and reference sheets",
            Icons.Default.Image,openImage) }
        item { DestinationCard("Animation","Image-to-video and ordered keyframes in the integrated video studio",
            Icons.Default.VideoLibrary,openAnimation) }
    }
}

@Composable
fun ImageGateway(openArtwork:()->Unit,openDefaults:()->Unit,openSheets:()->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text("IMAGE",color=Gold,style=MaterialTheme.typography.labelMedium) }
        item { Text("Choose a workflow",style=MaterialTheme.typography.headlineLarge) }
        item { DestinationCard("Artwork creation","Combine character, style, theme and element sheets into one artwork",
            Icons.Default.Image,openArtwork) }
        item { DestinationCard("Creation Studios","Characters, outfits, weapons, forms, creatures and artistic transformations",
            Icons.Default.AutoAwesome,openDefaults) }
        item { DestinationCard("Sheet creation","Build normalized Character, Art Style, Theme and Element reference sheets",
            Icons.Default.Image,openSheets) }
    }
}

@Composable
fun MethodologyGateway(onSelect:(CreationMode)->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("CREATION STUDIOS",color=Gold,style=MaterialTheme.typography.labelMedium) }
        item { Text("Choose your studio",style=MaterialTheme.typography.headlineMedium) }
        items(CreationMode.entries.toList()) { mode ->
            val spec=MethodologyRegistry.modes.getValue(mode)
            val layout=spec.layout
            DestinationCard(spec.name,
                if(mode==CreationMode.STYLE_CORRECTION)
                    "One target style sheet and the illustration to modify"
                else if(mode==CreationMode.CHARACTER)
                    "Concept-driven · 15 distinct characters · 3 sheets of 5"
                else layout.totalDesigns.toString()+" designs · exact "+layout.ratio+
                    " analytical catalogue",
                Icons.Default.AutoAwesome) { onSelect(mode) }
        }
    }
}

@Composable
fun MethodologyStudio(
    repo: ArtRepository,
    method: CreationMode,
    browseSheets:()->Unit,
    browseCore:()->Unit,
    openParameters:()->Unit,
    onQueued:()->Unit
) {
    val context=LocalContext.current
    val spec=MethodologyRegistry.modes.getValue(method)
    val catalog=remember { ConceptCatalog.load(context) }
    var letter by remember(method) { mutableStateOf("All") }
    var selectedConcepts by remember(method) { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSheets by remember(method) { mutableStateOf<Set<String>>(emptySet()) }
    var illustration by remember(method) { mutableStateOf<String?>(null) }
    var media by remember(method) { mutableStateOf(RenderMode.ANIME) }
    var failure by remember(method) { mutableStateOf<String?>(null) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onSuccess { illustration=uri.toString(); failure=null }
                .onFailure { failure="Unable to retain illustration access" }
        }
    }
    val concepts=if(method==CreationMode.ALTERNATE_UNIVERSE) {
        listOf("Dark fantasy","Cyberpunk","Wuxia / Xianxia","Post-apocalyptic",
            "Science fiction","Gothic","Historical","Urban occult","Steampunk","High fashion",
            "Space opera","Military fantasy").mapIndexed { i,v ->
            SelectableConcept("AU"+i,v)
        }
    } else catalog
    val displayed=if(letter=="All") concepts.take(70)
        else concepts.filter { it.label.startsWith(letter,true) }
    val countByRole=repo.sheets.filter { it.id in selectedSheets }.groupingBy { it.role }.eachCount()
    val required=spec.required
    val problems=MethodologyRegistry.validate(method,countByRole,selectedConcepts.size,
        illustration!=null)
    val sourcesReady=repo.sheets.filter { it.id in selectedSheets }.all { it.outputUri!=null }
    val ready=problems.isEmpty() && sourcesReady
    val showConcepts=spec.minimumConcepts>0
    val choices=repo.sheets.filter { it.role in required }

    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item { Text("METHOD-SPECIFIC DEFAULT",color=Gold,style=MaterialTheme.typography.labelMedium) }
        item { Text(spec.name,style=MaterialTheme.typography.headlineLarge) }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("OUTPUT CONTRACT",color=Gold)
                    Text(if(spec.layout.ratio=="SOURCE") "Match source image framing"
                        else spec.layout.ratio+" · "+spec.layout.totalDesigns+
                            " designs · "+spec.layout.pageCount+" sheet(s)",color=White)
                    Text(spec.directive,color=Mist,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (method!=CreationMode.CHARACTER) item {
            Text("OUTPUT TYPE",color=Gold)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(listOf(RenderMode.ANIME,RenderMode.REALISTIC,RenderMode.COSPLAY,
                        RenderMode.OTHER)) { type ->
                    FilterChip(selected=type==media,onClick={media=type},label={Text(type.display)})
                }
            }
        }
        if(showConcepts) {
            item {
                Text(if(method==CreationMode.CHARACTER)
                    "PRIMARY CONCEPTS · SELECT 2 OR MORE" else "SELECT A DESIGN CONCEPT",
                    color=Gold,style=MaterialTheme.typography.labelMedium)
                Text("Selected: "+selectedConcepts.size,color=Mist)
                if(selectedConcepts.isNotEmpty()) Text(selectedConcepts.mapNotNull { id ->
                    concepts.firstOrNull { it.id==id }?.label
                }.joinToString(" × "),color=White)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    items(listOf("All") + ('A'..'Z').map { it.toString() }) { symbol ->
                        FilterChip(selected=letter==symbol,onClick={letter=symbol},
                            label={Text(symbol)})
                    }
                }
            }
            items(displayed,key={it.id}) { concept ->
                val chosen=concept.id in selectedConcepts
                OutlinedCard(
                    modifier=Modifier.fillMaxWidth(),
                    border=BorderStroke(1.dp,if(chosen)Gold else Raised),
                    onClick={
                        selectedConcepts=if(chosen)selectedConcepts-concept.id
                        else selectedConcepts+concept.id
                    }
                ) {
                    Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text(if(chosen)"✓" else "+",color=Gold)
                        Text(concept.label,style=MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Text(concept.id,color=Mist,style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if(required.isNotEmpty()) {
            item {
                Text("SOURCE AUTHORITIES",color=Gold)
                Text(required.joinToString(" · ") { it.display }+
                    if(method==CreationMode.STYLE_CORRECTION)
                        " — no character or theme sheet needed" else "",
                    color=Mist)
                OutlinedButton(onClick=browseSheets) { Text("Create reference sheet") }
                if(SheetRole.CHARACTER in required)
                    OutlinedButton(onClick=browseCore) { Text("Find characters in Asterion Core") }
            }
            if(choices.isEmpty()) item { Text("No matching sheets yet.",color=Mist) }
            items(choices,key={it.id}) { sheet ->
                val chosen=sheet.id in selectedSheets
                OutlinedCard(
                    modifier=Modifier.fillMaxWidth(),
                    onClick={
                        selectedSheets=if(chosen) selectedSheets-sheet.id
                        else {
                            val single=selectedSheets - repo.sheets.filter {
                                it.role==sheet.role
                            }.map { it.id }.toSet()
                            single + sheet.id
                        }
                    },
                    border=BorderStroke(1.dp,if(chosen)Gold else Raised)
                ) {
                    Row(Modifier.padding(10.dp),
                        horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        AsyncImage(model=Uri.parse(sheet.outputUri ?:
                            sheet.sourceUris.first()),contentDescription=sheet.label,
                            modifier=Modifier.size(60.dp),contentScale=ContentScale.Fit)
                        Column {
                            Text(sheet.label,color=White)
                            Text(sheet.role.display,color=Mist)
                            if(chosen) Text("SELECTED",color=Gold)
                        }
                    }
                }
            }
        }
        if(method==CreationMode.STYLE_CORRECTION ||
            method in listOf(CreationMode.OUTFIT,CreationMode.WEAPON,
                CreationMode.TRANSFORMATION,CreationMode.MUTATION,
                CreationMode.ALTERNATE_UNIVERSE)) {
            item {
                Text(if(method==CreationMode.STYLE_CORRECTION)
                    "SOURCE ILLUSTRATION · REQUIRED" else "SOURCE ILLUSTRATION · OPTIONAL",color=Gold)
                OutlinedButton(onClick={picker.launch(arrayOf("image/*"))}) {
                    Text(if(illustration==null)"Choose illustration" else "Change illustration")
                }
                if(illustration!=null) {
                    AsyncImage(model=Uri.parse(illustration),contentDescription="Source illustration",
                        modifier=Modifier.size(140.dp),contentScale=ContentScale.Fit)
                }
            }
        }
        item {
            OutlinedButton(onClick=openParameters) { Text("Advanced generation controls") }
        }
        item {
            if(problems.isNotEmpty()) Text(problems.joinToString("; "),color=Red)
            if(failure!=null) Text(failure.orEmpty(),color=Red)
            Button(enabled=ready,modifier=Modifier.fillMaxWidth(),onClick={
                runCatching {
                    val settings=repo.settings.copy(
                        ratio=if(spec.layout.ratio=="SOURCE")repo.settings.ratio else spec.layout.ratio,
                        batch=spec.layout.pageCount
                    )
                    val picked=selectedConcepts.mapNotNull { id ->
                        concepts.firstOrNull { it.id==id }?.label
                    }
                    val job=GenerationJobStore(context).create(
                        media,selectedSheets.toList(),settings,method,illustration,picked)
                    val task=repo.recordTask(job.id,TaskKind.STILL_IMAGE)
                    GenerationWorker.enqueue(context,job.id,task.id)
                }.onSuccess { onQueued() }
                    .onFailure { failure=it.message }
            }) {
                androidx.compose.material3.Icon(Icons.Default.PlayArrow,null)
                Spacer(Modifier.width(8.dp))
                Text("Queue "+spec.name)
            }
        }
        item {
            Text("This mode follows its own reference, layout and candidate constraints. " +
                "Results still require device-level NPU support and visual validation.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
        }
    }
}
