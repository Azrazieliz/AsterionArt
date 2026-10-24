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
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetRole
import com.asterion.art.model.GradedAsset
import com.asterion.art.model.SheetLayouts
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskStatus
import com.asterion.art.render.SheetRenderer
import com.asterion.art.work.SheetRenderWorker
import com.asterion.art.work.GenerationWorker
import kotlinx.coroutines.delay
import com.asterion.art.learning.UserPreferenceStore
import com.asterion.art.methodology.MethodologyRegistry
import kotlin.math.roundToInt

private enum class Page(val label: String) {
    HOME("Home"), CREATE("Create"), SHEET("Sheet builder"), VIDEO("Video"), MODELS("Models"), CORE("Core sheets"), EDIT("Edit"), LIBRARY("Library"),
    JOBS("Jobs"), GRADING("Grading"), SETTINGS("Parameters")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterionArtApp(repo: ArtRepository) {
    AsterionArtTheme {
        var current by rememberSaveable { mutableStateOf(Page.HOME) }
        var editingAsset by remember { mutableStateOf<GradedAsset?>(null) }
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
                        if (current != Page.HOME) IconButton(onClick = { current = Page.HOME }) {
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
                        Triple(Page.LIBRARY, Icons.Default.Collections, "Sheets"),
                        Triple(Page.MODELS, Icons.Default.Tune, "Models"),
                        Triple(Page.JOBS, Icons.Default.WorkHistory, "Queue")
                    ).forEach { (page, icon, text) ->
                        NavigationBarItem(
                            selected = current == page,
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
                    Page.CREATE -> CreationScreen(repo) { current = it }
                    Page.SHEET -> SheetBuilderScreen(repo) { current = it }
                    Page.CORE -> CoreSheetBrowser(repo) { current = Page.JOBS }
                    Page.VIDEO -> VideoWorkspace(repo) { current = Page.JOBS }
                    Page.MODELS -> NativeModelLibraryScreen()
                    Page.LIBRARY -> SheetLibraryScreen(repo)
                    Page.JOBS -> QueueScreen(repo)
                    Page.GRADING -> GradingScreen(repo) { asset ->
                        editingAsset = asset
                        current = Page.EDIT
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Header(
                "Local · Phone-first",
                "Create from references",
                "Select authoritative sheets, build them from your reference images, and review saved artwork."
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navigate(Page.SHEET) },
                colors = CardDefaults.cardColors(containerColor = Raised),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Gold, modifier = Modifier.size(30.dp))
                    Text("Build a master sheet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Deterministic layouts from actual images: Character, Art Style, Theme, Element.",
                        color = Mist
                    )
                    Text("OPEN SHEET BUILDER →", color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Sheets", repo.sheets.size.toString(), Modifier.weight(1f))
                SummaryCard("Finished jobs",
                    repo.tasks.count { it.status == TaskStatus.COMPLETED }.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { navigate(Page.CREATE) }, modifier = Modifier.weight(1f)
                ) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(7.dp)); Text("Artwork") }
                OutlinedButton(onClick = { navigate(Page.GRADING) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Star, null); Spacer(Modifier.width(7.dp)); Text("Grade")
                }
            }
        }
        item {
            OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = Panel)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    val context = LocalContext.current
                    val bundled = EmbeddedNativeRunner.isEmbedded(context)
                    Text("ENGINE STATUS", color = Gold, style = MaterialTheme.typography.labelMedium)
                    Text(if(bundled) "Native engine bundled" else "Native engine unavailable",
                        fontWeight = FontWeight.SemiBold)
                    Text("Installed QNN checkpoints: " + NativeModelLibrary(context).list().size,
                        color = Mist)
                    OutlinedButton(onClick = { navigate(Page.MODELS) }) {
                        Text("Open native model library")
                    }
                }
            }
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
                sources.forEach { assignment ->
                    Text(SheetLayouts.label(kind, assignment.second), color = Mist,
                        style = MaterialTheme.typography.bodySmall)
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
            Header("Automated creation", "Image and video",
                "Choose your existing sheets. The generation engine will compile them through five controlled stages when available.")
        }
        item {
            OutlinedButton(onClick = { navigate(Page.CORE) }) { Text("Find character sheets in Asterion Core") }
        }
        item {
            OutlinedButton(onClick = { navigate(Page.VIDEO) }) {
                Text("Open integrated video workspace")
            }
        }
        item {
            Text("OUTPUT MODE", color = Gold, style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RenderMode.entries.toList()) { item ->
                    FilterChip(selected = mode == item, onClick = { mode = item }, label = { Text(item.display) })
                }
            }
        }
        item {
            Text("CREATION METHODOLOGY", color=Gold,
                style=MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected=creationMode==null,
                        onClick={creationMode=null},label={Text("Standard artwork")})
                }
                items(CreationMode.entries.toList()) { workflow ->
                    FilterChip(selected=creationMode==workflow,
                        onClick={creationMode=workflow},
                        label={Text(workflow.display)})
                }
            }
        }
        if(creationMode==CreationMode.STYLE_CORRECTION) {
            item {
                OutlinedButton(onClick={sourcePicker.launch(arrayOf("image/*"))}) {
                    Text(if(sourceArtwork==null)
                        "Choose source illustration to preserve"
                        else "Change source illustration")
                }
            }
        }
        if(sourceWarning!=null) item { Text(sourceWarning.orEmpty(),color=Red) }
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
                    Text(if (ready) "Five sequential inference passes use the on-device Local Dream compatibility backend until the native engine is embedded."
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
                        Text("Queue five-pass generation")
                    }
                }
            }
        }
        item {
            Text("Native engine currently requires Local Dream's on-device host mode. " +
                "A missing model or model context is reported as a failed job, never silently ignored. " +
                "Full engine embedding and video are still under development. " +
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
            "Sheet rendering continues via Android WorkManager. Your results remain in the local catalog.") }
        if (repo.tasks.isEmpty()) item { Text("No jobs submitted yet.", color = Mist) }
        items(repo.tasks.sortedByDescending { it.createdAt }) { task ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(task.kind.name.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                    Text(task.status.name, color = when (task.status) {
                        TaskStatus.COMPLETED -> Green
                        TaskStatus.FAILED -> Red
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
                            if (task.kind in setOf(TaskKind.STILL_IMAGE,TaskKind.IMAGE_EDIT,TaskKind.VIDEO))
                                wm.cancelAllWorkByTag("asterion-job-" + task.targetId)
                            else if (task.kind==TaskKind.SHEET_RENDER)
                                wm.cancelUniqueWork("asterion-sheet-" + task.targetId)
                            else wm.cancelUniqueWork("asterion-model-import-serial")
                            repo.updateTask(task.id, TaskStatus.FAILED, "Cancelled by user")
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
private fun GradingScreen(repo: ArtRepository, onEdit: (GradedAsset) -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            val good = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.isSuccess
            if (good) repo.registerAsset(uri.toString())
            else status = "Could not retain access to an image."
        }
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            Header("Manual feedback", "Grade results",
                "Your scores persist locally. The automatic quality model is not yet integrated and will not invent a score.")
        }
        item {
            Button(onClick = { picker.launch(arrayOf("image/*")) }) {
                Icon(Icons.Default.AddPhotoAlternate, null)
                Spacer(Modifier.width(8.dp))
                Text("Import images to grade")
            }
        }
        if (status != null) item { Text(status.orEmpty(), color = Red) }
        if (repo.assets.isEmpty()) item { Text("No images available to rate.", color = Mist) }
        items(repo.assets.sortedByDescending { it.createdAt }) { asset ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = Uri.parse(asset.uri), contentDescription = "Image for manual rating",
                        modifier = Modifier.fillMaxWidth().height(235.dp), contentScale = ContentScale.Fit
                    )
                    if (asset.autoScore != null) {
                        Text("Technical screening: " + "%.0f".format(asset.autoScore) + "/100 " +
                            "(contrast/detail only, not character or style fidelity)", color = Mist)
                    }
                    if (asset.sourceJobId != null) {
                        OutlinedButton(onClick = { onEdit(asset) }) {
                            Text("Edit selected pixels")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your rating", Modifier.weight(1f), color = Mist)
                        (1..5).forEach { score ->
                            IconButton(onClick = { repo.setStars(asset.uri, score) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    if (score <= asset.stars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$score stars",
                                    tint = if (score <= asset.stars) Gold else Mist
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParametersScreen(repo: ArtRepository) {
    var local by remember(repo.settings) { mutableStateOf(repo.settings) }
    fun save(value: com.asterion.art.model.GeneratorSettings) {
        local = value
        repo.saveSettings(value)
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header("Advanced controls", "Generation parameters",
            "Editable control profiles for the future generation engine. Values are not assumed to be ideal for every model.") }
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
