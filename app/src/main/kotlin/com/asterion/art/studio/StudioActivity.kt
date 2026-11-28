package com.asterion.art.studio

import android.content.Intent
import android.content.ClipData
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.asterion.art.BuildConfig
import com.asterion.art.creation.*
import com.asterion.art.runtime.ArtWorkspace
import com.asterion.art.runtime.AnimaStudy
import com.asterion.art.runtime.CreationService
import com.asterion.art.runtime.NEXUS_DOCUMENTS
import com.asterion.art.runtime.NativeAnima
import com.asterion.art.runtime.ProjectionProbe
import com.asterion.art.runtime.ROUNDER_DOCUMENTS
import com.asterion.art.runtime.familyName
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Foundation = Color(0xFF111118)
private val Panel = Color(0xFF201E29)
private val Lilac = Color(0xFFDABEFF)
private val Outline = Color(0xFF3E3749)
private val Violet = Color(0xFF34234A)
private val Palette = darkColorScheme(
    primary = Lilac, onPrimary = Color(0xFF2A173C), secondary = Color(0xFFD3BACB),
    surface = Foundation, surfaceContainer = Panel, background = Foundation,
    onSurface = Color(0xFFF4EDF8), onSurfaceVariant = Color(0xFFC5B9CB), outlineVariant = Outline,
)

class StudioActivity : ComponentActivity() {
    private var incoming by mutableStateOf<Uri?>(null)
    private var incomingScene by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        receive(intent)
        setContent {
            MaterialTheme(colorScheme = Palette) {
                Studio(ArtWorkspace.get(this), incoming, incomingScene) {
                    incoming = null
                    incomingScene = null
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receive(intent)
    }

    private fun receive(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        if (intent.type == "text/plain" || intent.type == "text/markdown") {
            incomingScene = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf(String::isNotBlank)
            return
        }
        incoming = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}

private enum class Destination(val label: String) { CREATE("Create"), PROJECTS("Projects"), MODELS("Models") }

private val Mode.isVideo get() = this == Mode.CLIP || this == Mode.ANIMATION
private val Mode.canReview get() = !isVideo && this != Mode.THEME

private data class ArtworkSelection(val mode: Mode, val role: RefRole)

@Composable
private fun Studio(workspace: ArtWorkspace, incoming: Uri?, incomingScene: String?, handled: () -> Unit) {
    val context = LocalContext.current
    val drafts = remember { Drafts(context) }
    val scope = rememberCoroutineScope()
    val messages = remember { SnackbarHostState() }
    var destination by rememberSaveable { mutableStateOf(Destination.CREATE) }
    var activeMode by rememberSaveable { mutableStateOf<Mode?>(null) }
    var openProject by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by remember(activeMode) { mutableStateOf(activeMode?.let(drafts::load)) }
    var selectedRole by remember { mutableStateOf(RefRole.SOURCE) }
    var runs by remember { mutableStateOf(emptyList<CreationRun>()) }
    var editingReference by remember { mutableStateOf<Int?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var artworkSelection by remember { mutableStateOf<ArtworkSelection?>(null) }
    val artworkProvider = remember {
        setOf(ROUNDER_DOCUMENTS, NEXUS_DOCUMENTS).any { context.packageManager.resolveContentProvider(it, 0) != null }
    }

    fun update(next: StudioDraft) {
        drafts.write(next)
        draft = next
    }

    fun select(mode: Mode) {
        destination = Destination.CREATE
        activeMode = mode
        draft = drafts.load(mode)
        selectedRole = roles(mode).first()
    }

    fun importReference(uri: Uri, mode: Mode, role: RefRole) = scope.launch {
        val imported = runCatching { withContext(Dispatchers.IO) { workspace.references.import(uri) } }
        imported.fold(
            onSuccess = { asset ->
                val target = if (draft?.mode == mode) checkNotNull(draft) else drafts.load(mode)
                val next = target.copy(references = target.references + Reference(role, asset))
                if (draft?.mode == mode) update(next) else drafts.write(next)
            },
            onFailure = { messages.showSnackbar(it.message ?: "The reference could not be opened") },
        )
    }

    val pickDocument = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        val mode = activeMode
        if (uri != null && mode != null) importReference(uri, mode, selectedRole)
    }
    LaunchedEffect(incoming) {
        if (incoming != null) {
            pendingUri = incoming
            handled()
            if (activeMode == null) select(Mode.CHARACTER_SHEET)
        }
    }
    LaunchedEffect(incomingScene) {
        if (incomingScene != null) {
            select(Mode.ANIMATION)
            update(StudioDraft(Mode.ANIMATION, direction = incomingScene))
            handled()
        }
    }
    LaunchedEffect(workspace) {
        while (true) {
            runs = withContext(Dispatchers.IO) { workspace.journal.list() }
            delay(if (runs.any { it.status == RunStatus.RUNNING }) 500 else 1400)
        }
    }

    Scaffold(
        containerColor = Foundation,
        snackbarHost = { SnackbarHost(messages) },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = {
                            if (item == Destination.CREATE && destination == item) activeMode = null
                            if (item == Destination.PROJECTS && destination == item) openProject = null
                            destination = item
                        },
                        icon = {
                            Text(
                                when (item) {
                                    Destination.CREATE -> "✦"
                                    Destination.PROJECTS -> "▤"
                                    Destination.MODELS -> "▥"
                                },
                                fontSize = 19.sp,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { inset ->
        Box(Modifier.fillMaxSize().padding(inset)) {
            when (destination) {
                Destination.CREATE -> {
                    val current = draft
                    if (current == null) ModeLibrary(::select)
                    else CreationEditor(
                        current, workspace.taxonomy, workspace, ::update,
                        onBack = { activeMode = null },
                        onChoose = { role ->
                            selectedRole = role
                            val shared = pendingUri
                            if (shared != null) {
                                pendingUri = null
                                importReference(shared, current.mode, role)
                            } else {
                                pickDocument.launch(if (role == RefRole.AUDIO) arrayOf("audio/*") else arrayOf("image/*"))
                            }
                        },
                        onChooseArtwork = if (artworkProvider) {
                            { role -> artworkSelection = ArtworkSelection(current.mode, role) }
                        } else null,
                        onEditReference = { editingReference = it },
                        onStart = {
                            scope.launch {
                                runCatching {
                                    val request = current.request()
                                    withContext(Dispatchers.IO) { workspace.journal.create(request) }
                                }.onSuccess { run ->
                                    runs = listOf(run) + runs
                                    openProject = run.id
                                    destination = Destination.PROJECTS
                                    runCatching { CreationService.wake(context) }.onFailure {
                                        messages.showSnackbar("Saved to the queue. Open Projects to resume.")
                                    }
                                }.onFailure {
                                    messages.showSnackbar(it.message ?: "Check the creation inputs")
                                }
                            }
                        },
                    )
                }
                Destination.PROJECTS -> Projects(
                    runs, openProject, workspace, drafts.revisions(),
                    onOpen = { openProject = it },
                    onResume = { run ->
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    when (run.status) {
                                        RunStatus.AWAITING_DESIGN -> workspace.journal.approveDesign(run.id)
                                        RunStatus.QUEUED -> Unit
                                        else -> workspace.journal.resume(run.id)
                                    }
                                }
                                CreationService.wake(context)
                            }.onFailure { messages.showSnackbar(it.message ?: "The creation is still queued") }
                        }
                    },
                    onCancel = { id -> scope.launch { withContext(Dispatchers.IO) { workspace.journal.cancel(id) } } },
                    onShare = { run, asset ->
                        scope.launch {
                            runCatching {
                                val file = withContext(Dispatchers.IO) {
                                    workspace.outputFiles(run.id).verified(asset)
                                }
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.outputs", file)
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = asset.mediaType
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    clipData = ClipData.newRawUri(run.request.mode.title, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, null))
                            }.onFailure {
                                messages.showSnackbar(it.message ?: "The output could not be opened")
                            }
                        }
                    },
                    onSaveCandidate = { id, sha ->
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { workspace.journal.retainCandidate(id, sha) } }
                                .onFailure { messages.showSnackbar("This candidate has already been cleared") }
                        }
                    },
                    onTheme = { theme ->
                        select(Mode.THEME_SHEET)
                        update(StudioDraft(Mode.THEME_SHEET, theme = theme))
                    },
                    onUseImage = { run, artifact, mode ->
                        select(mode)
                        val asset = AssetRef(
                            artifact.sha256, AssetSource.ART_OUTPUT, "${run.id}/${artifact.sha256}",
                            "Selected ${run.request.mode.title} design", artifact.sha256,
                        )
                        val selected = checkNotNull(draft)
                        if (mode == Mode.ARTWORK) {
                            val theme = (run.request.input as? SheetInput)?.theme?.cast
                            val references = selected.references.filter { it.role != RefRole.THEME } + Reference(RefRole.THEME, asset)
                            update(selected.copy(references = references, cast = theme?.copy(authority = asset.id)))
                        } else {
                            val baseIdentity = if (mode == Mode.CHARACTER_SHEET && run.request.mode == Mode.TRANSFORMATION) {
                                run.request.references.firstOrNull { it.role in setOf(RefRole.CHARACTER, RefRole.SOURCE) }?.asset?.id
                            } else null
                            update(StudioDraft(mode, references = listOf(Reference(RefRole.SOURCE, asset)), baseIdentityId = baseIdentity))
                        }
                    },
                    openDraft = { saved ->
                        select(saved.mode)
                        update(saved)
                    },
                    onEdit = { run, index, state ->
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { workspace.journal.editDesign(run, index, state) } }
                                .onFailure { messages.showSnackbar(it.message ?: "This design could not be saved") }
                        }
                    },
                )
                Destination.MODELS -> Models(workspace)
            }
        }
    }

    val current = draft
    artworkSelection?.let { selection ->
        ArtworkPicker(onDismiss = { artworkSelection = null }, onSelect = { uri ->
            artworkSelection = null
            importReference(uri, selection.mode, selection.role)
        })
    }
    val referenceIndex = editingReference
    val reference = current?.references?.getOrNull(referenceIndex ?: -1)
    if (reference != null && referenceIndex != null) {
        ReferenceDialog(
            reference,
            onDismiss = { editingReference = null },
            onApply = { replacement ->
                val currentRefs = current.references.toMutableList()
                if (replacement == null) currentRefs.removeAt(referenceIndex) else currentRefs[referenceIndex] = replacement
                val remaining = currentRefs.map { it.asset.id }.toSet()
                val timeline = current.shots.map {
                    it.copy(
                        openingFrameId = it.openingFrameId?.takeIf(remaining::contains),
                        endingFrameId = it.endingFrameId?.takeIf(remaining::contains),
                    )
                }
                update(current.copy(references = currentRefs, shots = timeline, audio = current.audio.filter { it.assetId in remaining }))
                editingReference = null
            },
        )
    }
}

@Composable
private fun ModeLibrary(open: (Mode) -> Unit) {
    val originals = Mode.entries.filter {
        it == Mode.CHARACTER || it == Mode.THEME || it in OriginalInput.ORIGINALS
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Wordmark("Asterion Art") }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Violet, Panel)))
                    .clickable { open(Mode.ARTWORK) }.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Artwork", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
                Text("Open studio  ↗", color = Lilac, style = MaterialTheme.typography.labelLarge)
            }
        }
        item { Section("Originals") }
        items(originals.chunked(2)) { row -> ModeRow(row, open) }
        item { Section("Reference sheets") }
        items(listOf(Mode.CHARACTER_SHEET, Mode.THEME_SHEET, Mode.STYLE_SHEET).chunked(2)) { row ->
            ModeRow(row, open)
        }
        item { Section("Motion") }
        item { ModeRow(listOf(Mode.CLIP, Mode.ANIMATION), open) }
    }
}

@Composable
private fun ModeRow(modes: List<Mode>, open: (Mode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.forEach { mode ->
            Surface(
                onClick = { open(mode) }, modifier = Modifier.weight(1f).heightIn(min = 82.dp),
                shape = RoundedCornerShape(18.dp), color = Panel,
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("↗", color = Lilac, style = MaterialTheme.typography.labelMedium)
                    Text(mode.title, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        if (modes.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CreationEditor(
    draft: StudioDraft,
    taxonomy: CharacterTaxonomy,
    workspace: ArtWorkspace,
    change: (StudioDraft) -> Unit,
    onBack: () -> Unit,
    onChoose: (RefRole) -> Unit,
    onChooseArtwork: ((RefRole) -> Unit)?,
    onEditReference: (Int) -> Unit,
    onStart: () -> Unit,
) {
    var details by rememberSaveable(draft.mode) { mutableStateOf(false) }
    var role by remember(draft.mode) { mutableStateOf(roles(draft.mode).first()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onBack, contentPadding = PaddingValues(0.dp)) { Text("‹  Create") }
            Wordmark(draft.mode.title)
        }
        if (draft.mode == Mode.CHARACTER) {
            item { ConceptSelector(taxonomy, draft, change) }
        }
        if (draft.mode == Mode.THEME) {
            item {
                Field("Keywords", draft.keywords, { change(draft.copy(keywords = it)) }, false)
                draft.theme?.let { Text(it.title) }
            }
        }
        if (draft.mode.isVideo) item {
            if (draft.mode == Mode.ANIMATION) {
                Field("Scene specification", draft.direction, { change(draft.copy(direction = it)) }, false)
            }
            Timeline(draft, change)
        }
        else item {
            Field(
                if (draft.mode == Mode.THEME) "Visual direction" else "Direction",
                draft.direction, { change(draft.copy(direction = it)) }, false,
            )
        }
        if (draft.mode in setOf(Mode.ARTWORK, Mode.CHARACTER, Mode.OUTFIT, Mode.EQUIPMENT)) {
            item {
                val placeholder = when (draft.mode) {
                    Mode.ARTWORK -> "3"
                    Mode.CHARACTER -> if (draft.characterOperation == CharacterOperation.CREATE) "5" else "1"
                    else -> "10"
                }
                Field(
                    if (draft.mode == Mode.ARTWORK) "Candidates per phase" else "Design count",
                    draft.count, { change(draft.copy(count = it)) },
                    keyboard = KeyboardType.Number, placeholder = placeholder,
                )
            }
        }
        item {
            Section("References")
            Choice("Reference role", roles(draft.mode), role, ::roleLabel) { role = it }
            OutlinedButton(onClick = { onChoose(role) }) { Text("Add reference") }
            if (onChooseArtwork != null && role != RefRole.AUDIO && role != RefRole.GEOMETRY) {
                TextButton(onClick = { onChooseArtwork(role) }) { Text("Browse artwork library") }
            }
        }
        items(draft.references.withIndex().toList(), key = { "${it.index}-${it.value.asset.id}" }) { indexed ->
            val reference = indexed.value
            Surface(onClick = { onEditReference(indexed.index) }, color = Panel, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val file = remember(reference) { runCatching { workspace.references.path(reference.asset) }.getOrNull() }
                    AsyncImage(file, null, Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(reference.asset.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(roleLabel(reference.role), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Edit", color = Lilac)
                }
            }
        }
        if (draft.mode == Mode.ARTWORK) item { CastEditor(draft, change) }
        if (draft.mode == Mode.THEME_SHEET && draft.theme != null) item { Text(checkNotNull(draft.theme).title) }
        item {
            OutlinedButton(onClick = { details = !details }) { Text(if (details) "Hide options" else "Options") }
            if (details) Options(draft, change)
        }
        item {
            Button(onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                Text(if (draft.route == Route.DESIGN_FIRST) "Prepare design" else "Start creation")
            }
        }
    }
}

@Composable
private fun ConceptSelector(taxonomy: CharacterTaxonomy, draft: StudioDraft, change: (StudioDraft) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf<String?>(null) }
    var browsing by remember { mutableStateOf(false) }
    fun add(slot: String) = change(draft.copy(slots = listOf(draft.slots, slot).filter(String::isNotBlank).joinToString(" × ")))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Field("Concepts", draft.slots, { change(draft.copy(slots = it)) }, placeholder = "Concept × Concept")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { browsing = !browsing }) { Text(if (browsing) "Hide taxonomy" else "Browse taxonomy") }
            OutlinedButton(onClick = { add("X") }) { Text("Random concept") }
        }
        if (browsing) {
            Field("Find a concept", search, { search = it })
            if (search.isNotBlank()) {
                taxonomy.searchLeaves(search).take(12).forEach { id ->
                    val item = taxonomy.leaves.getValue(id)
                    TextButton(onClick = { add(id) }) { Text("${item.name}  ·  $id") }
                }
            } else {
                val active = branch?.let(taxonomy.nodes::get)
                if (active != null) {
                    TextButton(onClick = {
                        val previous = taxonomy.nodes.entries.firstOrNull { branch in it.value.children }?.key
                        branch = previous
                    }) { Text("‹  ${active.name}") }
                    OutlinedButton(onClick = { add("X{$branch}") }) { Text("Draw from ${active.name}") }
                }
                (active?.children ?: taxonomy.roots).forEach { id ->
                    val node = taxonomy.nodes.getValue(id)
                    TextButton(onClick = { branch = id }) { Text("${node.name}  ·  ${taxonomy.descendants(id).size}  ›") }
                }
                active?.leaves?.forEach { id ->
                    TextButton(onClick = { add(id) }) { Text(taxonomy.leaves.getValue(id).name) }
                }
            }
        }
    }
}

@Composable
private fun Options(draft: StudioDraft, change: (StudioDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (draft.mode != Mode.THEME) {
            Choice("Render family", Family.entries, draft.family, ::familyName) { change(draft.copy(family = it)) }
            Choice("Quality", Quality.entries, draft.quality, { it.label() }) { change(draft.copy(quality = it)) }
        }
        if (draft.mode.canReview) {
            Choice("Workflow", Route.entries, draft.route, { if (it == Route.DIRECT) "Direct" else "Design first" }) {
                change(draft.copy(route = it))
            }
        }
        when (draft.mode) {
            Mode.ARTWORK -> Field(
                "Creative freedom", draft.creativeFreedom, { change(draft.copy(creativeFreedom = it)) },
                keyboard = KeyboardType.Decimal, placeholder = "Automatic",
            )
            Mode.THEME -> {
                Field("Beauty", draft.beauty, { change(draft.copy(beauty = it)) }, keyboard = KeyboardType.Decimal)
                Field("Eroticism", draft.eroticism, { change(draft.copy(eroticism = it)) }, keyboard = KeyboardType.Decimal)
                Field("Tension", draft.tension, { change(draft.copy(tension = it)) }, keyboard = KeyboardType.Decimal)
            }
            Mode.CHARACTER -> Choice("Operation", CharacterOperation.entries, draft.characterOperation, { it.label() }) {
                change(draft.copy(characterOperation = it))
            }
            Mode.OUTFIT -> Toggle("Construction views", draft.constructionViews) { change(draft.copy(constructionViews = it)) }
            Mode.TRANSFORMATION -> Toggle("Progression stages", draft.progression) { change(draft.copy(progression = it)) }
            Mode.FEMINISATION -> Field(
                "Reconstruction profile", draft.profile, { change(draft.copy(profile = it)) },
                placeholder = "Level 4.5",
            )
            Mode.MUTATION -> VisualAttribute.entries.forEach { attribute ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(attribute.label(), Modifier.weight(1f))
                    val value = draft.retention[attribute]
                    TextButton(onClick = {
                        val next = if (value == null) draft.retention + (attribute to 1f) else draft.retention - attribute
                        change(draft.copy(retention = next))
                    }) { Text(if (value == null) "Set retention" else "Automatic") }
                }
                draft.retention[attribute]?.let { value ->
                    Slider(value, { change(draft.copy(retention = draft.retention + (attribute to it))) })
                }
            }
            else -> Unit
        }
        if (draft.mode in OriginalInput.FIVE_DESIGN_CATALOGUES) {
            Choice(
                "Presentation", listOf<OriginalPresentation?>(null) + OriginalPresentation.entries,
                draft.presentation, { it?.label() ?: "Automatic" },
            ) { change(draft.copy(presentation = it)) }
        }
    }
}

@Composable
private fun Timeline(draft: StudioDraft, change: (StudioDraft) -> Unit) {
    fun edit(index: Int, shot: ShotDraft) = change(draft.copy(shots = draft.shots.toMutableList().apply { set(index, shot) }))
    val frames = draft.references.filter { it.role == RefRole.KEYFRAME }.map { it.asset }.distinctBy { it.id }
    val audioFiles = draft.references.filter { it.role == RefRole.AUDIO }.map { it.asset }.distinctBy { it.id }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        draft.shots.forEachIndexed { index, shot ->
            Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Section("Shot ${index + 1}")
                    Field("Scene", shot.scene, { edit(index, shot.copy(scene = it)) }, false)
                    Field("Seconds", shot.seconds, { edit(index, shot.copy(seconds = it)) }, keyboard = KeyboardType.Decimal)
                    Field("Camera", shot.camera, { edit(index, shot.copy(camera = it)) })
                    Field("Motion", shot.motion, { edit(index, shot.copy(motion = it)) })
                    if (frames.isNotEmpty()) {
                        val choices = listOf<AssetRef?>(null) + frames
                        Choice("Opening frame", choices, frames.firstOrNull { it.id == shot.openingFrameId }, { it?.name ?: "Automatic" }) {
                            edit(index, shot.copy(openingFrameId = it?.id))
                        }
                        Choice("Ending frame", choices, frames.firstOrNull { it.id == shot.endingFrameId }, { it?.name ?: "Automatic" }) {
                            edit(index, shot.copy(endingFrameId = it?.id))
                        }
                    }
                    Row {
                        TextButton({ change(draft.copy(shots = draft.shots.filterNot { it.id == shot.id })) }) { Text("Remove") }
                        if (index > 0) TextButton({
                            val edited = draft.shots.toMutableList()
                            edited.add(index - 1, edited.removeAt(index))
                            change(draft.copy(shots = edited))
                        }) { Text("Move earlier") }
                    }
                }
            }
        }
        if (draft.shots.isEmpty() || draft.mode == Mode.ANIMATION) {
            OutlinedButton(onClick = {
                change(draft.copy(shots = draft.shots + ShotDraft(UUID.randomUUID().toString())))
            }) { Text("Add shot") }
        }
        if (audioFiles.isNotEmpty()) {
            Section("Audio cues")
            draft.audio.forEachIndexed { index, cue ->
                val asset = audioFiles.firstOrNull { it.id == cue.assetId } ?: return@forEachIndexed
                fun editCue(next: AudioDraft) = change(draft.copy(audio = draft.audio.toMutableList().apply { set(index, next) }))
                Choice("Audio source", audioFiles, asset, { it.name }) { editCue(cue.copy(assetId = it.id)) }
                Field("Start time in seconds", cue.seconds, { editCue(cue.copy(seconds = it)) }, keyboard = KeyboardType.Decimal)
                Toggle("Dialogue", cue.dialogue) { editCue(cue.copy(dialogue = it)) }
                TextButton(onClick = {
                    change(draft.copy(audio = draft.audio.toMutableList().apply { removeAt(index) }))
                }) { Text("Remove cue") }
            }
            OutlinedButton(onClick = {
                change(draft.copy(audio = draft.audio + AudioDraft(audioFiles.first().id)))
            }) { Text("Add audio cue") }
        }
    }
}

@Composable
private fun CastEditor(draft: StudioDraft, change: (StudioDraft) -> Unit) {
    val theme = draft.references.firstOrNull { it.role == RefRole.THEME }?.asset ?: return
    val characters = draft.references.filter { it.role == RefRole.CHARACTER }.map { it.asset }.distinctBy { it.id }
    val cast = draft.cast?.takeIf { it.authority == theme.id }
        ?: ThemeCast(theme.id, 0, emptyList())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section("Theme cast")
        cast.subjects.forEachIndexed { index, subject ->
            val source = characters.firstOrNull { it.id == subject.characterAuthority }
            if (characters.isNotEmpty()) {
                Choice("Character Sheet", listOf<AssetRef?>(null) + characters, source, { it?.name ?: "Unassigned" }) {
                    val subjects = cast.subjects.toMutableList().apply { set(index, subject.copy(characterAuthority = it?.id)) }
                    change(draft.copy(cast = cast.copy(subjects = subjects)))
                }
            }
            Field("Theme clothing override", subject.clothingOverride.orEmpty(), { value ->
                val subjects = cast.subjects.toMutableList().apply { set(index, subject.copy(clothingOverride = value.ifBlank { null })) }
                change(draft.copy(cast = cast.copy(subjects = subjects)))
            })
            TextButton(onClick = {
                val subjects = cast.subjects.filterNot { it.id == subject.id }
                change(draft.copy(cast = cast.copy(specifiedCount = subjects.size, subjects = subjects)))
            }) { Text("Remove subject") }
        }
        if (characters.isNotEmpty()) OutlinedButton(onClick = {
            val subjects = cast.subjects + ThemeSubject(UUID.randomUUID().toString(), characters.first().id)
            change(draft.copy(cast = cast.copy(specifiedCount = subjects.size, subjects = subjects)))
        }) { Text("Add Theme subject") }
        Toggle("Random male subject", cast.randomMaleSubject != null) { enabled ->
            val male = if (enabled) UUID.randomUUID().toString() else null
            change(draft.copy(cast = cast.copy(randomMaleSubject = male)))
        }
        if (cast.randomMaleSubject != null) characters.forEach { candidate ->
            Toggle(candidate.name, candidate.id in draft.malePool) { eligible ->
                val pool = if (eligible) draft.malePool + candidate.id else draft.malePool - candidate.id
                change(draft.copy(malePool = pool))
            }
        }
        if (draft.cast == null) {
            TextButton(onClick = { change(draft.copy(cast = cast)) }) { Text("Use this Theme's configuration") }
        }
    }
}

@Composable
private fun ReferenceDialog(reference: Reference, onDismiss: () -> Unit, onApply: (Reference?) -> Unit) {
    var subject by remember { mutableStateOf(reference.subject.orEmpty()) }
    var priority by remember { mutableStateOf(reference.priority.toString()) }
    var region by remember {
        val values = reference.region?.let { listOf(it.left, it.top, it.right, it.bottom) }
        mutableStateOf(values?.joinToString(", ").orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(reference.asset.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Subject", subject, { subject = it })
                Field("Priority", priority, { priority = it }, keyboard = KeyboardType.Number)
                Field("Region · left, top, right, bottom", region, { region = it }, placeholder = "0, 0, 1, 1")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    val bounds = region.takeIf(String::isNotBlank)?.split(',')?.map { it.trim().toFloat() }
                    val assigned = bounds?.let {
                        require(it.size == 4)
                        Region(it[0], it[1], it[2], it[3])
                    }
                    reference.copy(priority = priority.toInt(), subject = subject.ifBlank { null }, region = assigned)
                }.onSuccess(onApply).onFailure { error = "Use a nonnegative priority and four ordered values from 0 to 1." }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { onApply(null) }) { Text("Remove reference") } },
    )
}

@Composable
private fun Projects(
    runs: List<CreationRun>,
    selected: String?,
    workspace: ArtWorkspace,
    drafts: List<StudioDraft>,
    onOpen: (String?) -> Unit,
    onResume: (CreationRun) -> Unit,
    onCancel: (String) -> Unit,
    onShare: (CreationRun, ContentArtifact) -> Unit,
    onSaveCandidate: (String, String) -> Unit,
    onTheme: (ThemeDesign) -> Unit,
    onUseImage: (CreationRun, ContentArtifact, Mode) -> Unit,
    openDraft: (StudioDraft) -> Unit,
    onEdit: (String, Int, DesignState) -> Unit,
) {
    val run = runs.firstOrNull { it.id == selected }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (run == null) {
            item { Wordmark("Projects") }
            if (runs.isEmpty() && drafts.isEmpty()) item { Text("Your creations will appear here.") }
            items(runs, key = { it.id }) { item ->
                Surface(onClick = { onOpen(item.id) }, shape = RoundedCornerShape(18.dp), color = Panel) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text(item.request.mode.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.status.label(), color = Lilac, style = MaterialTheme.typography.bodySmall)
                        item.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            if (drafts.isNotEmpty()) item { Section("Drafts") }
            items(drafts, key = { "draft-${it.id}" }) { saved ->
                Surface(onClick = { openDraft(saved) }, shape = RoundedCornerShape(18.dp), color = Panel) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text(saved.mode.title, style = MaterialTheme.typography.titleMedium)
                        val summary = saved.theme?.title ?: saved.slots.ifBlank { saved.direction }
                        if (summary.isNotBlank()) Text(summary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            return@LazyColumn
        }
        item {
            TextButton(onClick = { onOpen(null) }) { Text("‹  Projects") }
            Wordmark(run.request.mode.title)
            Text(run.status.label(), color = Lilac)
            run.reason?.let { Text(it) }
            if (run.status == RunStatus.RUNNING) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
        }
        if (run.status == RunStatus.AWAITING_DESIGN) {
            items(run.designs.size) { index ->
                val state = run.designEdits[index] ?: run.designs[index].state
                PreparedCard(index, state) { onEdit(run.id, index, it) }
            }
        }
        items(run.themes, key = { it.id }) { theme -> ThemeCard(theme) { onTheme(theme) } }
        val finished = ((run.finalImage?.let(::listOf) ?: run.independentImages) + run.savedImages)
            .distinctBy { it.artifact.sha256 }
        items(finished.withIndex().toList(), key = { "${it.index}-${it.value.artifact.sha256}" }) { image ->
            OutputCard(run, image.value.artifact, workspace) {
                Column {
                    when (run.request.mode) {
                        Mode.CHARACTER, Mode.ARTWORK, in OriginalInput.ORIGINALS -> {
                            SheetMenu { onUseImage(run, image.value.artifact, it) }
                        }
                        Mode.THEME_SHEET -> {
                            TextButton(onClick = { onUseImage(run, image.value.artifact, Mode.ARTWORK) }) {
                                Text("Use Theme Sheet in Artwork")
                            }
                        }
                        else -> Unit
                    }
                    TextButton(onClick = { onShare(run, image.value.artifact) }) { Text("Share") }
                }
            }
        }
        val displayed = finished.map { it.artifact.sha256 }.toSet()
        val retainedWinners = run.phases
            .filter { it.winner.artifact.sha256 !in displayed }
            .distinctBy { it.winner.artifact.sha256 }
        if (retainedWinners.isNotEmpty()) item { Section("Phase winners") }
        items(retainedWinners, key = { "winner-${it.phase}" }) { phase ->
            OutputCard(run, phase.winner.artifact, workspace) {
                Text(phase.phase.label(), modifier = Modifier.padding(8.dp))
            }
        }
        items(run.pendingPhase.distinctBy { it.artifact.sha256 }, key = { it.artifact.sha256 }) { pending ->
            OutputCard(run, pending.artifact, workspace) {
                TextButton(onClick = { onSaveCandidate(run.id, pending.artifact.sha256) }) { Text("Keep candidate") }
            }
        }
        run.video?.let { video ->
            item { OutlinedButton(onClick = { onShare(run, video) }) { Text("Share video") } }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (run.status in setOf(RunStatus.QUEUED, RunStatus.PAUSED, RunStatus.BLOCKED, RunStatus.FAILED, RunStatus.AWAITING_DESIGN)) {
                    Button(onClick = { onResume(run) }) {
                        Text(if (run.status == RunStatus.AWAITING_DESIGN) "Generate approved design" else "Resume")
                    }
                }
                if (run.status !in setOf(RunStatus.COMPLETE, RunStatus.CANCELLED)) {
                    OutlinedButton(onClick = { onCancel(run.id) }) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun SheetMenu(create: (Mode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text("Create reference sheet") }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            listOf(Mode.CHARACTER_SHEET, Mode.THEME_SHEET, Mode.STYLE_SHEET).forEach { mode ->
                DropdownMenuItem(text = { Text(mode.title) }, onClick = {
                    open = false
                    create(mode)
                })
            }
        }
    }
}

@Composable
private fun PreparedCard(index: Int, state: DesignState, save: (DesignState) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }) { Text("Design ${index + 1}  ${if (expanded) "−" else "+"}") }
            if (expanded) {
                state.subjects.forEachIndexed { subjectIndex, subject ->
                    subject.fields.forEach { (field, fact) ->
                        Field(field.label(), fact.value, { value ->
                            if (value.isNotBlank()) {
                                val subjects = state.subjects.toMutableList()
                                subjects[subjectIndex] = subject.copy(fields = subject.fields + (field to fact.copy(value = value)))
                                save(state.copy(subjects = subjects))
                            }
                        }, false)
                    }
                }
                state.scene.forEach { (field, fact) ->
                    Field(field.label(), fact.value, { value ->
                        if (value.isNotBlank()) save(state.copy(scene = state.scene + (field to fact.copy(value = value))))
                    }, false)
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: ThemeDesign, use: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(theme.title, style = MaterialTheme.typography.titleMedium)
            Text(theme.introduction)
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide specification" else "Show specification") }
            if (expanded) theme.visualSystems.forEach { (field, fact) ->
                Text(field.label(), color = Lilac)
                Text(fact.value)
            }
            OutlinedButton(use) { Text("Create Theme Sheet") }
        }
    }
}

@Composable
private fun OutputCard(run: CreationRun, artifact: ContentArtifact, workspace: ArtWorkspace, actions: @Composable () -> Unit) {
    val source = File(workspace.directory, "outputs/${run.id}/${artifact.sha256}")
    Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth()) {
            AsyncImage(source, "Generated image", Modifier.fillMaxWidth().heightIn(max = 480.dp), contentScale = ContentScale.Fit)
            Box(Modifier.padding(horizontal = 8.dp)) { actions() }
        }
    }
}

@Composable
private fun Models(workspace: ArtWorkspace) {
    val context = LocalContext.current
    val nexusAvailable = remember { context.packageManager.resolveContentProvider(NEXUS_DOCUMENTS, 0) != null }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Wordmark("Models")
            Text(if (nexusAvailable) "Nexus document provider available" else "Nexus document provider unavailable")
        }
        items(Family.entries) { family ->
            Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(familyName(family), style = MaterialTheme.typography.titleMedium)
                    Text("Production runtime pending", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (BuildConfig.DEBUG) item { ProjectionCheck(workspace) }
        if (BuildConfig.DEBUG && NativeAnima.available) item { AnimaOutfitStudy(workspace) }
    }
}

@Composable
private fun AnimaOutfitStudy(workspace: ArtWorkspace) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val study = remember(workspace) { AnimaStudy(context, workspace) }
    var ready by remember { mutableStateOf(study.isStaged()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf(Drafts(context).load(Mode.OUTFIT).direction) }
    var selectedRole by rememberSaveable { mutableStateOf(RefRole.ELEMENT) }
    var references by remember { mutableStateOf(emptyList<Reference>()) }
    var image by remember { mutableStateOf<File?>(null) }
    val chooseModels = rememberLauncherForActivityResult(OpenDocumentTree()) { directory ->
        if (directory != null) scope.launch {
            busy = true
            status = "Checking model files"
            status = runCatching {
                withContext(Dispatchers.IO) {
                    val operation = currentCoroutineContext()
                    study.stage(directory, ExecutionSignal { operation.ensureActive() })
                }
                "Model kit verified"
            }.getOrElse { it.message ?: "Model import failed" }
            ready = study.isStaged()
            busy = false
        }
    }
    val chooseReference = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val role = selectedRole
            runCatching { withContext(Dispatchers.IO) { workspace.references.import(uri) } }.fold(
                onSuccess = { asset -> references = references + Reference(role, asset) },
                onFailure = { status = it.message ?: "Reference import failed" },
            )
            busy = false
        }
    }
    Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Anima Outfit study", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(enabled = !busy, onClick = { chooseModels.launch(null) }) {
                Text(if (ready) "Change Nexus model kit" else "Choose Nexus model kit")
            }
            Field("Outfit direction", direction, { direction = it }, singleLine = false)
            for ((role, label) in listOf(RefRole.ELEMENT to "motif", RefRole.OUTFIT to "surface")) {
                OutlinedButton(enabled = !busy, onClick = {
                    selectedRole = role
                    chooseReference.launch(arrayOf("image/*"))
                }) { Text("Add $label reference") }
            }
            for (reference in references) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reference.asset.name, Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { references = references - reference }) { Text("Remove") }
                }
            }
            Button(enabled = ready && !busy && direction.isNotBlank(), onClick = {
                scope.launch {
                    busy = true
                    status = "Rendering on this device"
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val operation = currentCoroutineContext()
                            study.render(direction, references, ExecutionSignal { operation.ensureActive() })
                        }
                    }.fold(
                        onSuccess = { result ->
                            image = result.image
                            val usage = result.conditioning
                            status = "${usage.textPositions} text positions, ${usage.conditioningPositions} renderer positions"
                        },
                        onFailure = { status = it.message ?: "Outfit study failed" },
                    )
                    busy = false
                }
            }) { Text("Render Outfit study") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (status.isNotBlank()) Text(status)
            image?.let { AsyncImage(it, "Rendered Outfit study", Modifier.fillMaxWidth().heightIn(max = 420.dp)) }
        }
    }
}

@Composable
private fun ProjectionCheck(workspace: ArtWorkspace) {
    val context = LocalContext.current
    val probe = remember(workspace) { ProjectionProbe(context, workspace) }
    val scope = rememberCoroutineScope()
    var staged by remember { mutableStateOf(probe.stagedParts()) }
    var target by rememberSaveable { mutableStateOf<ProjectionProbe.Part?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val part = target
            status = runCatching {
                withContext(Dispatchers.IO) {
                    if (part == null) probe.check(uri) else {
                        probe.stage(part, uri)
                        "${part.label} verified"
                    }
                }
            }.getOrElse { it.message ?: "The projection check could not complete" }
            staged = probe.stagedParts()
            busy = false
        }
    }
    Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reference projection check", style = MaterialTheme.typography.titleMedium)
            ProjectionProbe.Part.entries.forEach { part ->
                OutlinedButton(enabled = !busy, onClick = {
                    target = part
                    pick.launch(arrayOf("application/octet-stream", "application/x-onnx"))
                }) { Text(if (part in staged) "${part.label} · staged" else "Select ${part.label.lowercase()}") }
            }
            Button(enabled = !busy && probe.canCheck(staged), onClick = {
                target = null
                pick.launch(arrayOf("image/*"))
            }) { Text("Check a reference") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (status.isNotBlank()) Text(status)
        }
    }
}

@Composable
private fun Wordmark(title: String) {
    Text(
        title, style = MaterialTheme.typography.headlineLarge,
        fontFamily = FontFamily.Serif, modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun Section(title: String) {
    Text(title, color = Lilac, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun Field(
    label: String,
    value: String,
    write: (String) -> Unit,
    singleLine: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
) {
    OutlinedTextField(
        value, write, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = singleLine, minLines = if (singleLine) 1 else 2,
        shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

@Composable
private fun <T> Choice(label: String, choices: List<T>, selected: T, title: (T) -> String, choose: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text("${title(selected)}  ▾") }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { item ->
                    DropdownMenuItem(text = { Text(title(item)) }, onClick = { choose(item); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, set: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = set)
    }
}

private fun roleLabel(role: RefRole) = when (role) {
    RefRole.CHARACTER -> "Character Sheet"
    RefRole.THEME -> "Theme Sheet"
    RefRole.ART_STYLE -> "Art Style Sheet"
    RefRole.ELEMENT -> "Legacy Element Sheet"
    RefRole.SOURCE -> "Source image"
    else -> role.label()
}

private fun roles(mode: Mode): List<RefRole> = when (mode) {
    Mode.ARTWORK -> listOf(RefRole.CHARACTER, RefRole.THEME, RefRole.ART_STYLE, RefRole.ELEMENT, RefRole.POSE)
    Mode.THEME -> listOf(RefRole.SOURCE, RefRole.POSE, RefRole.ELEMENT)
    Mode.CLIP, Mode.ANIMATION -> listOf(RefRole.KEYFRAME, RefRole.CHARACTER, RefRole.ART_STYLE, RefRole.AUDIO)
    Mode.STYLE_CORRECTION -> listOf(RefRole.SOURCE, RefRole.ART_STYLE)
    Mode.EQUIPMENT -> listOf(RefRole.CHARACTER, RefRole.OUTFIT, RefRole.SOURCE)
    else -> listOf(RefRole.SOURCE, RefRole.CHARACTER, RefRole.POSE, RefRole.ART_STYLE)
}

private fun Enum<*>.label() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)
