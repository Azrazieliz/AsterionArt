package com.asterion.art.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.model.ArtPolicies
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import com.asterion.art.nativeengine.EmbeddedNativeRunner
import com.asterion.art.nativeengine.NativeModelLibrary
import com.asterion.art.nativeengine.NativeInferenceProfiles
import com.asterion.art.nativeengine.MemoryProfile
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetRole
import com.asterion.art.model.GradedAsset
import com.asterion.art.model.SheetLayouts
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskStatus
import com.asterion.art.render.SheetRenderer
import com.asterion.art.work.NeuralEnhancementWorker
import com.asterion.art.enhancement.EmbeddedRealSr
import com.asterion.art.work.SheetRenderWorker
import com.asterion.art.work.GenerationWorker
import kotlinx.coroutines.delay
import com.asterion.art.learning.UserPreferenceStore
import com.asterion.art.integrations.InstalledImageEnhancers
import com.asterion.art.methodology.MethodologyRegistry
import kotlin.math.roundToInt

private enum class Page(val label: String) {
    HOME("Home"), CREATE("Create"), IMAGE_GATE("Image"), ARTWORK("Artwork"), DEFAULTS("Defaults"), DEFAULT_STUDIO("Methodology"), SHEET("Sheet builder"), VIDEO("Video"), MODELS("Models"), CORE("Core sheets"), EDIT("Edit"), TOOLBOX("Toolbox"), LIBRARY("Library"),
    JOBS("Jobs"), GRADING("Grading"), TEST("Testing"), SETTINGS("Parameters")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterionArtApp(repo: ArtRepository) {
    AsterionArtTheme {
        var current by rememberSaveable { mutableStateOf(Page.HOME) }
        var editingAsset by remember { mutableStateOf<GradedAsset?>(null) }
        var selectedDefault by remember { mutableStateOf(CreationMode.CHARACTER) }
        Scaffold(
            containerColor = Ink,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("ASTERION", fontWeight = FontWeight.Bold, color = Gold)
                            Text("ART STUDIO", style = MaterialTheme.typography.labelSmall, color = Mist)
                        }
                    },
                    navigationIcon = {
                        if (current != Page.HOME) IconButton(onClick = {
                            current = when(current) {
                                Page.DEFAULT_STUDIO -> Page.DEFAULTS
                                Page.DEFAULTS,Page.ARTWORK,Page.SHEET -> Page.IMAGE_GATE
                                Page.IMAGE_GATE,Page.VIDEO -> Page.CREATE
                                else -> Page.HOME
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Home")
                        }
                    },
                    actions = {
                        IconButton(onClick = { current = Page.SETTINGS }) {
                            Icon(Icons.Default.Tune, contentDescription = "Parameters")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    listOf(
                        Triple(Page.HOME, Icons.Default.Home, "Home"),
                        Triple(Page.CREATE, Icons.Default.AutoAwesome, "Create"),
                        Triple(Page.LIBRARY, Icons.Default.Collections, "Library")
                    ).forEach { (page, icon, text) ->
                        NavigationBarItem(
                            selected = if(page==Page.CREATE)
                                current in setOf(Page.CREATE,Page.IMAGE_GATE,Page.ARTWORK,
                                    Page.DEFAULTS,Page.DEFAULT_STUDIO,Page.VIDEO)
                                else current == page,
                            onClick = { current = page },
                            icon = { Icon(icon, contentDescription = text) },
                            label = { Text(text) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (current) {
                    Page.HOME -> HomeScreen(repo) { current = it }
                    Page.CREATE -> CreativeGateway(
                        openImage={ current=Page.IMAGE_GATE },
                        openAnimation={ current=Page.VIDEO })
                    Page.IMAGE_GATE -> ImageGateway(
                        openArtwork={ current=Page.ARTWORK },
                        openDefaults={ current=Page.DEFAULTS },
                        openSheets={ current=Page.SHEET })
                    Page.ARTWORK -> CreationScreen(repo) { current=it }
                    Page.DEFAULTS -> MethodologyGateway { method ->
                        selectedDefault=method
                        current=Page.DEFAULT_STUDIO
                    }
                    Page.DEFAULT_STUDIO -> MethodologyStudioV3(repo,selectedDefault,
                        browseSheets={ current=Page.SHEET },
                        browseCore={ current=Page.CORE },
                        openParameters={ current=Page.SETTINGS },
                        onQueued={ current=Page.HOME })
                    Page.SHEET -> SheetBuilderScreen(repo) { current = it }
                    Page.CORE -> CoreSheetBrowser(repo) { current = Page.HOME }
                    Page.VIDEO -> VideoWorkspace(repo) { current = Page.HOME }
                    Page.MODELS -> NativeModelLibraryScreen()
                    Page.TEST -> TestingCenter(repo,
                        openModels={current=Page.MODELS},
                        openSheets={current=Page.SHEET},
                        openArtwork={current=Page.ARTWORK},
                        openVideo={current=Page.VIDEO},
                        openGallery={current=Page.LIBRARY},
                        openGrading={current=Page.GRADING},
                        openCore={current=Page.CORE})
                    Page.LIBRARY -> LibraryHub(repo,
                        onEdit={ asset -> editingAsset=asset;current=Page.EDIT },
                        onToolbox={ asset -> editingAsset=asset;current=Page.TOOLBOX })
                    Page.JOBS -> QueueScreen(repo)
                    Page.GRADING -> AdvancedGradingScreen(repo,
                        onEdit = { asset ->
                            editingAsset = asset
                            current = Page.EDIT
                        },
                        onToolbox = { asset ->
                            editingAsset = asset
                            current = Page.TOOLBOX
                        })
                    Page.TOOLBOX -> ImageToolboxStudio(editingAsset, repo) {
                        current = Page.JOBS
                    }
                    Page.EDIT -> SelectiveEditScreen(editingAsset, repo) { current = Page.JOBS }
                    Page.SETTINGS -> ParametersScreen(repo)
                }
            }
        }
    }
}

@Composable
private fun Header(kicker: String, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelMedium, color = Gold)
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold, color = White)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = Mist)
    }
}

@Composable
private fun HomeScreen(repo: ArtRepository, navigate: (Page) -> Unit) {
    val context=LocalContext.current
    val pending=repo.tasks.filter { it.status==TaskStatus.QUEUED ||
        it.status==TaskStatus.RUNNING }
    val latest=repo.assets.sortedByDescending { it.createdAt }.take(6)
    val models=remember(repo.tasks) { NativeModelLibrary(context).list() }
    LaunchedEffect(pending.isNotEmpty()) {
        while(pending.isNotEmpty()) {
            delay(1100L)
            repo.reload()
        }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp)) {
        item { Header("CREATIVE STUDIO","Asterion Art",
            "Generate artwork, design new characters and animate images.") }
        item {
            Card(modifier=Modifier.fillMaxWidth().clickable { navigate(Page.CREATE) },
                colors=CardDefaults.cardColors(containerColor=Raised),
                shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Default.AutoAwesome,null,tint=Gold,modifier=Modifier.size(32.dp))
                    Text("Start creating",style=MaterialTheme.typography.headlineSmall)
                    Text("Choose image or animation",color=Mist)
                    Text("OPEN STUDIO →",color=Gold)
                }
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                FilledTonalButton(onClick={navigate(Page.IMAGE_GATE)},modifier=Modifier.weight(1f)){
                    Icon(Icons.Default.Image,null)
                    Spacer(Modifier.width(7.dp))
                    Text("Image")
                }
                OutlinedButton(onClick={navigate(Page.VIDEO)},modifier=Modifier.weight(1f)){
                    Icon(Icons.Default.PlayArrow,null)
                    Spacer(Modifier.width(7.dp))
                    Text("Animation")
                }
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                SummaryCard("Sheets",repo.sheets.size.toString(),Modifier.weight(1f))
                SummaryCard("Models",models.size.toString(),Modifier.weight(1f))
                SummaryCard("In queue",pending.size.toString(),Modifier.weight(1f))
            }
        }
        if(pending.isNotEmpty()) item {
            OutlinedCard(onClick={navigate(Page.JOBS)},
                colors=CardDefaults.outlinedCardColors(containerColor=Panel)) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("BACKGROUND ACTIVITY",color=Gold)
                    pending.take(3).forEach { task ->
                        Text(task.kind.name.replace('_',' ')+" · "+(task.stage ?: task.status.name))
                        if(task.progressTotal>0) {
                            LinearProgressIndicator(progress={
                                (task.progressCurrent.toFloat()/task.progressTotal).coerceIn(0f,1f)
                            },modifier=Modifier.fillMaxWidth())
                        } else LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                    }
                    Text("OPEN QUEUE →",color=Gold)
                }
            }
        }
        if(latest.isNotEmpty()) {
            item { Text("RECENT RESULTS",color=Gold,style=MaterialTheme.typography.labelMedium) }
            item {
                LazyRow(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    items(latest) { asset ->
                        Card(onClick={navigate(Page.GRADING)},modifier=Modifier.size(108.dp,138.dp),
                            colors=CardDefaults.cardColors(containerColor=Panel)) {
                            AsyncImage(model=Uri.parse(asset.uri),contentDescription="Artwork",
                                modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        }
                    }
                }
            }
        }
        item {
            Text("LIBRARIES",color=Gold)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={navigate(Page.CORE)},modifier=Modifier.weight(1f)){
                    Text("Asterion Core")
                }
                OutlinedButton(onClick={navigate(Page.MODELS)},modifier=Modifier.weight(1f)){
                    Text("Model library")
                }
            }
            OutlinedButton(onClick={navigate(Page.GRADING)},modifier=Modifier.fillMaxWidth()){
                Icon(Icons.Default.Star,null)
                Spacer(Modifier.width(8.dp))
                Text("Review and edit artworks")
            }
            Button(onClick={navigate(Page.TEST)},modifier=Modifier.fillMaxWidth()){
                Text("Run actual app and AI tests")
            }
        }
        item {
            Text(if(EmbeddedNativeRunner.isEmbedded(context))
                "Embedded NPU runtime detected · compatible model required"
                else "Native inference executable not found",
                color=Mist,style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SummaryCard(label: String, count: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(15.dp)) {
            Text(count, style = MaterialTheme.typography.headlineMedium, color = Gold)
            Text(label, color = Mist, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SheetBuilderScreen(repo: ArtRepository, navigate: (Page) -> Unit) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(SheetRole.CHARACTER) }
    var sources by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
    var activeSlot by remember { mutableStateOf("PRIMARY") }
    var note by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = SheetRenderer.MAX_REFS - sources.size
            if (uris.size > remaining) {
                note = "No images omitted: only " + remaining + " reference slots remain. Select fewer files."
            } else {
                val approved = uris.all { uri ->
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }.isSuccess
                }
                if (approved) {
                    val existingUris = sources.map { it.first }.toSet()
                    sources = sources + uris.filter { it.toString() !in existingUris }
                        .map { it.toString() to activeSlot }
                    note = null
                } else {
                    note = "Unable to retain reference URI permission through the document picker."
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Header(
                "Normalized production",
                "Sheet builder",
                "Choose a sheet type and real reference images. The renderer creates a standard sheet without inventing visual details."
            )
        }
        item {
            OutlinedButton(onClick = { navigate(Page.CORE) }) { Text("Browse Asterion Core character sheets") }
        }
        item {
            Text("SHEET TYPE", color = Gold, style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SheetRole.entries.toList()) { role ->
                    FilterChip(
                        selected = kind == role, onClick = {
                            kind = role; activeSlot = "PRIMARY"; sources = emptyList()
                        },
                        label = { Text(role.display) }
                    )
                }
            }
        }
        item {
            Text("ASSIGN REFERENCE TO PANEL", color = Gold, style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SheetLayouts.forRole(kind)) { slot ->
                    FilterChip(
                        selected = activeSlot == slot.id,
                        onClick = { activeSlot = slot.id },
                        label = { Text(slot.label) }
                    )
                }
            }
            Text("Selected images are assigned to " + SheetLayouts.label(kind, activeSlot) +
                ". Primary is mandatory; optional panels only appear when you supply images.",
                color = Mist, style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedCard(
                onClick = { picker.launch(arrayOf("image/*")) },
                colors = CardDefaults.outlinedCardColors(containerColor = Panel),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Gold, modifier = Modifier.size(30.dp))
                    Column {
                        Text("Choose reference images", fontWeight = FontWeight.Medium)
                        Text(sources.size.toString() + " / " + SheetRenderer.MAX_REFS +
                            " selected · Android files", color = Mist)
                    }
                }
            }
        }
        if (sources.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sources) { uri ->
                        Card(
                            modifier = Modifier.size(126.dp, 160.dp),
                            colors = CardDefaults.cardColors(containerColor = Raised)
                        ) {
                            AsyncImage(
                                model = Uri.parse(uri.first), contentDescription = "Reference: " + uri.second,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
        if (sources.isNotEmpty()) {
            item {
                Text("Assignments", color = Gold)
                sources.forEachIndexed { index,assignment ->
                    var expanded by remember(kind,index,assignment.first) {
                        mutableStateOf(false)
                    }
                    Row(
                        modifier=Modifier.fillMaxWidth(),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(6.dp)
                    ) {
                        Text((index+1).toString()+". "+
                            SheetLayouts.label(kind,assignment.second),
                            modifier=Modifier.weight(1f),color=Mist,
                            style=MaterialTheme.typography.bodySmall)
                        Box {
                            TextButton(onClick={expanded=true}){Text("Reassign")}
                            DropdownMenu(expanded=expanded,
                                onDismissRequest={expanded=false}) {
                                SheetLayouts.forRole(kind).forEach { slot ->
                                    DropdownMenuItem(
                                        text={Text(slot.label)},
                                        onClick={
                                            sources=sources.mapIndexed { i,item ->
                                                if(i==index)item.first to slot.id else item
                                            }
                                            expanded=false
                                        })
                                }
                            }
                        }
                        TextButton(onClick={
                            sources=sources.filterIndexed{i,_->i!=index}
                        }){Text("Remove")}
                    }
                }
            }
        }
        if (note != null) {
            item { Text(note.orEmpty(), color = Red) }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = sources.any { it.second == "PRIMARY" },
                onClick = {
                    runCatching {
                        val sheet = repo.addSheet(kind, sources.map { it.first }, sources.map { it.second })
                        val task = repo.recordTask(sheet.id, TaskKind.SHEET_RENDER)
                        SheetRenderWorker.enqueue(context, sheet.id, task.id)
                    }.onSuccess {
                        sources = emptyList()
                        note = null
                        navigate(Page.JOBS)
                    }.onFailure { note = it.message }
                }
            ) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Build sheet in background") }
        }
        item {
            Text(
                "Source images are kept at their original URIs. No silent cropping, fake character traits or AI-generated annotations.",
                style = MaterialTheme.typography.bodySmall,
                color = Mist
            )
        }
    }
}

@Composable
private fun LibraryHub(
    repo: ArtRepository,
    onEdit: (GradedAsset) -> Unit,
    onToolbox: (GradedAsset) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf("Creations") }
    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp),
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("Creations","Designs","Sheets","Models","History")) { value ->
                FilterChip(selected=tab==value,onClick={tab=value},
                    label={Text(value)})
            }
        }
        Box(Modifier.weight(1f)) {
            when(tab) {
                "Creations" -> AdvancedGradingScreen(repo,onEdit=onEdit,onToolbox=onToolbox)
                "Designs" -> DesignLibraryScreen(repo)
                "Sheets" -> SheetLibraryScreen(repo)
                "Models" -> NativeModelLibraryScreen()
                else -> QueueScreen(repo)
            }
        }
    }
}

@Composable
private fun SheetLibraryScreen(repo: ArtRepository) {
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("Asterion Core bridge pending", "Sheet library",
            "Local sheets stay on this device. Core-backed sheet search will use the dedicated permissioned bridge.") }
        if (repo.sheets.isEmpty()) item { Text("No sheets built yet.", color = Mist) }
        items(repo.sheets.sortedByDescending { it.createdAt }) { sheet ->
            SheetGalleryCard(sheet)
        }
    }
}

@Composable
private fun SheetGalleryCard(sheet: SheetRecord) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = Uri.parse(sheet.outputUri ?: sheet.sourceUris.first()),
                contentDescription = sheet.label,
                modifier = Modifier.size(95.dp, 130.dp), contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(sheet.role.display.uppercase(), style = MaterialTheme.typography.labelSmall, color = Gold)
                Text(sheet.label, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(sheet.sourceUris.size.toString() + " verified source images", color = Mist)
                Text(if (sheet.outputUri != null) "Rendered" else "Rendering queued", color = if (sheet.outputUri != null) Green else Blue)
            }
        }
    }
}

@Composable
private fun CreationScreen(repo: ArtRepository, navigate: (Page) -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(RenderMode.ANIME) }
    var creationMode by remember { mutableStateOf<CreationMode?>(null) }
    var sourceArtwork by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sourceWarning by remember { mutableStateOf<String?>(null) }
    val sourcePicker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onSuccess { sourceArtwork=uri.toString(); sourceWarning=null }
                .onFailure { sourceWarning="Cannot keep source artwork access" }
        }
    }
    val grouped = repo.sheets.groupBy { it.role }
    val counts = repo.sheets.filter { it.id in selected }.groupingBy { it.role }.eachCount()
    val roleFailures=if(creationMode==null) {
        if(ArtPolicies.eligibleForAutomation(counts))emptyList()
        else listOf("Character, Art Style and Theme sheets are required")
    } else MethodologyRegistry.validate(requireNotNull(creationMode),counts)
    val ready = roleFailures.isEmpty() &&
        (creationMode!=CreationMode.STYLE_CORRECTION || sourceArtwork!=null)
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Header("Image generator", "Artwork creation",
                "Choose character, art style and theme sheets for your illustration. Elements are optional.")
        }
        item {
            OutlinedButton(onClick = { navigate(Page.CORE) }) { Text("Find character sheets in Asterion Core") }
        }

        item {
            Text("OUTPUT MODE", color = Gold, style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RenderMode.entries.toList()) { item ->
                    FilterChip(selected = mode == item, onClick = { mode = item }, label = { Text(item.display) })
                }
            }
        }
        SheetRole.entries.forEach { role ->
            item {
                Text(role.display.uppercase(), color = Gold, style = MaterialTheme.typography.labelMedium)
            }
            val these = grouped[role].orEmpty()
            if (these.isEmpty()) {
                item {
                    OutlinedButton(onClick = { navigate(Page.SHEET) }) {
                        Icon(Icons.Default.AddPhotoAlternate, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create " + role.display.lowercase() + " sheet")
                    }
                }
            } else {
                items(these) { sheet ->
                    val included = selected.contains(sheet.id)
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val withoutOther = if (role == SheetRole.ART_STYLE || role == SheetRole.THEME)
                                selected - these.map { it.id }.toSet()
                            else selected
                            selected = if (included) selected - sheet.id else withoutOther + sheet.id
                        },
                        colors = CardDefaults.outlinedCardColors(containerColor = if (included) Raised else Panel),
                        border = BorderStroke(1.dp, if (included) Gold else Raised)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = Uri.parse(sheet.outputUri ?: sheet.sourceUris.first()),
                                contentDescription = null,
                                modifier = Modifier.size(54.dp), contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(sheet.label, Modifier.weight(1f), maxLines = 2)
                            Checkbox(checked = included, onCheckedChange = null)
                        }
                    }
                }
            }
        }
        item {
            OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (ready) "References selected" else "Missing required sheets", fontWeight = FontWeight.Bold)
                    Text(if (ready)
                        "Uses Asterion Art's integrated local QNN engine. A compatible installed "+
                        "image model is required; actual output is verified by rendering."
                        else (roleFailures.joinToString("; ") + if (creationMode==CreationMode.STYLE_CORRECTION && sourceArtwork==null) "; Source illustration required" else ""), color = Mist)
                    FilledTonalButton(onClick = { navigate(Page.SETTINGS) }) {
                        Icon(Icons.Default.Tune, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Configure parameters")
                    }
                    Button(
                        enabled = ready && mode != RenderMode.ANIMATION &&
                            repo.sheets.filter { it.id in selected }.all { it.outputUri != null },
                        onClick = {
                            val preferred = if (repo.settings.automatic)
                                UserPreferenceStore(context).suggestion(
                                    mode,repo.settings,repo.assets)?.settings ?: repo.settings
                            else repo.settings
                            val job = GenerationJobStore(context).create(
                                mode, selected.toList(), preferred,creationMode,sourceArtwork)
                            val task = repo.recordTask(job.id, TaskKind.STILL_IMAGE)
                            GenerationWorker.enqueue(context, job.id, task.id)
                            navigate(Page.JOBS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Queue native artwork generation")
                    }
                }
            }
        }
        item {
            Text("Requires a compatible installed on-device QNN model. " +
                "A missing model or model context is reported as a failed job, never silently ignored. " +
                "The native executable is embedded; working inference still requires a compatible converted QNN model and device verification. " +
                "Animation uses a separately connected video backend. " +
                "Cosplay preserves canonical horns, wings, tails and other fantasy features.",
                color = Mist, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QueueScreen(repo: ArtRepository) {
    val context = LocalContext.current
    val active = repo.tasks.any { it.status == TaskStatus.QUEUED ||
        it.status == TaskStatus.RUNNING }
    LaunchedEffect(active) {
        while (active) {
            delay(900L)
            repo.reload()
        }
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("Background processing", "Job history",
            "Import, image, sheet, edit and video jobs persist through WorkManager and the local task catalog.") }
        if (repo.tasks.isEmpty()) item { Text("No jobs submitted yet.", color = Mist) }
        items(repo.tasks.sortedByDescending { it.createdAt }) { task ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(task.kind.name.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                    Text(task.status.name, color = when (task.status) {
                        TaskStatus.COMPLETED -> Green
                        TaskStatus.FAILED,TaskStatus.CANCELLED -> Red
                        else -> Blue
                    })
                    if (task.stage != null && task.status in listOf(TaskStatus.RUNNING,TaskStatus.QUEUED)) {
                        Text(task.stage, style = MaterialTheme.typography.bodySmall, color = Mist)
                        if (task.progressTotal > 0) {
                            val value = (task.progressCurrent.toFloat() /
                                task.progressTotal).coerceIn(0f,1f)
                            LinearProgressIndicator(progress = { value },
                                modifier = Modifier.fillMaxWidth())
                            Text((value * 100).toInt().toString() + "%", color = Gold)
                        } else {
                            LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                        }
                        if (task.progressBytesTotal > 0) {
                            Text((task.progressBytes/1048576L).toString() + " / " +
                                (task.progressBytesTotal/1048576L) + " MiB",
                                style=MaterialTheme.typography.bodySmall,color=Mist)
                        }
                    }
                    if (task.error != null) Text(task.error, color = Red)
                    Text(task.id.take(13), style = MaterialTheme.typography.bodySmall, color = Mist)
                    if (task.status == TaskStatus.RUNNING || task.status == TaskStatus.QUEUED) {
                        OutlinedButton(onClick = {
                            val wm=androidx.work.WorkManager.getInstance(context)
                            when {
                                task.targetId.startsWith("native-smoke-") ->
                                    wm.cancelAllWorkByTag("asterion-smoke-"+task.id)
                                task.kind==TaskKind.ENHANCEMENT ->
                                    wm.cancelAllWorkByTag("asterion-task-"+task.id)
                                task.kind==TaskKind.VIDEO ->
                                    wm.cancelUniqueWork(
                                        if(task.targetId.startsWith("join_"))
                                            "asterion-video-join-"+task.targetId
                                        else "asterion-video-"+task.targetId)
                                task.kind in setOf(TaskKind.STILL_IMAGE,TaskKind.IMAGE_EDIT) ->
                                    wm.cancelAllWorkByTag("asterion-job-"+task.targetId)
                                task.kind==TaskKind.SHEET_RENDER ->
                                    wm.cancelUniqueWork("asterion-sheet-"+task.targetId)
                                task.kind==TaskKind.MODEL_IMPORT ->
                                    wm.cancelAllWorkByTag("asterion-model-import-"+task.id)
                            }
                            repo.updateTask(task.id, TaskStatus.CANCELLED, "Cancelled by user")
                        }) { Text("Cancel") }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { repo.reload() }) { Text("Refresh job history") }
        }
    }
}

@Composable
private fun ParametersScreen(repo: ArtRepository) {
    val context=LocalContext.current
    var profile by remember { mutableStateOf(NativeInferenceProfiles.get(context)) }
    var local by remember(repo.settings) { mutableStateOf(repo.settings) }
    fun save(value: com.asterion.art.model.GeneratorSettings) {
        local = value
        repo.saveSettings(value)
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header("Advanced controls", "Generation parameters",
            "Model-dependent controls for inference. Stored preferences are not guaranteed optimal for every checkpoint.") }
        item {
            Text("ASPECT RATIO", color = Gold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ArtPolicies.selectableRatios) { ratio ->
                    FilterChip(selected = local.ratio == ratio,
                        onClick = { save(local.copy(ratio = ratio)) }, label = { Text(ratio) })
                }
            }
        }
        item {
            Text("NATIVE INFERENCE MEMORY",color=Gold)
            Text("Auto uses the safe mode. Performance attempts persistent QNN graphs " +
                "only with sufficient free RAM; it may fail on some model conversions.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(MemoryProfile.entries.toList()) { item ->
                    FilterChip(selected=item==profile,onClick={
                        NativeInferenceProfiles.set(context,item)
                        profile=item
                    },label={Text(item.label)})
                }
            }
        }
        item {
            Text("SAMPLER / SCHEDULER", color = Gold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ArtPolicies.schedulers) { method ->
                    FilterChip(selected = local.scheduler == method,
                        onClick = { save(local.copy(scheduler = method)) }, label = { Text(method) })
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Karras schedule", Modifier.weight(1f))
                Switch(checked = local.karras, onCheckedChange = { save(local.copy(karras = it)) })
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Automatic optimization", Modifier.weight(1f))
                Switch(checked = local.automatic, onCheckedChange = { save(local.copy(automatic = it)) })
            }
        }
        item {
            Text("Steps: " + local.steps, color = Gold)
            Slider(value = local.steps.toFloat(), onValueChange = {
                local = local.copy(steps = it.roundToInt())
            }, valueRange = 1f..100f, onValueChangeFinished = { repo.saveSettings(local) })
        }
        item {
            Text("CFG scale: " + "%.2f".format(local.cfg), color = Gold)
            Slider(value = local.cfg, onValueChange = { local = local.copy(cfg = it) },
                valueRange = 0f..20f, onValueChangeFinished = { repo.saveSettings(local) })
        }
        item {
            Text("Batch count: " + local.batch, color = Gold)
            Slider(value = local.batch.toFloat(), onValueChange = {
                local = local.copy(batch = it.roundToInt())
            }, valueRange = 1f..12f, onValueChangeFinished = { repo.saveSettings(local) })
        }
        item {
            Text("Image-to-image denoise: " + "%.2f".format(local.denoise), color = Gold)
            Slider(value = local.denoise, onValueChange = { local = local.copy(denoise = it) },
                valueRange = 0f..1f, onValueChangeFinished = { repo.saveSettings(local) })
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lock seed", Modifier.weight(1f), color = Mist)
                Switch(checked = local.seed != null, onCheckedChange = {
                    save(local.copy(seed = if (it) 12345L else null))
                })
            }
            if (local.seed != null) {
                Text("Seed: " + local.seed, color = Gold)
                Slider(value = local.seed!!.toFloat(), onValueChange = {
                    local = local.copy(seed = it.toLong())
                }, valueRange = 0f..1000000f,
                    onValueChangeFinished = { repo.saveSettings(local) })
            }
        }
    }
}
