package com.asterion.compiler.art

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState as rememberHorizontalScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asterion.compiler.ui.theme.AsterionTheme
import kotlinx.coroutines.launch
import java.util.UUID

/** First integrated Art screen: working reference selection, manual review,
 * Nightmare API image/video execution. Reference-aware four-sheet inference,
 * model embedding, Core bridge and automatic image analysis are NOT yet wired.
 */
@Composable
fun AsterionArtApp(onOpenLegacyCompiler: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheets = remember { mutableStateListOf<ReferenceSheet>() }
    val frames = remember { mutableStateListOf<StoryboardFrame>() }
    var selectedKind by remember { mutableStateOf(SheetKind.CHARACTER) }
    var mode by remember { mutableStateOf(ArtMode.ANIME) }
    var token by remember { mutableStateOf("") }
    var motion by remember { mutableStateOf("Character moves naturally with expressive body motion; maintain appearance and details.") }
    var status by remember { mutableStateOf("No job submitted") }
    var running by remember { mutableStateOf(false) }
    var latest by remember { mutableStateOf<NightmareResult?>(null) }
    var humanRating by remember { mutableStateOf(8f) }
    var technical by remember { mutableStateOf<Evidence?>(null) }
    var approximate by remember { mutableStateOf(false) }
    var candidateCount by remember { mutableStateOf(2f) }
    val candidates = remember { mutableStateListOf<NightmareResult>() }

    val sheetPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val item = ReferenceSheet(
                id = UUID.randomUUID().toString(),
                kind = selectedKind, uri = uri.toString(),
                title = uri.lastPathSegment?.substringAfterLast('/') ?: "Reference",
            )
            if (selectedKind != SheetKind.CHARACTER) sheets.removeAll { it.kind == selectedKind }
            sheets += item
        }
    }
    val framePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            frames += StoryboardFrame(UUID.randomUUID().toString(), uri.toString(), motion, 2)
        }
    }
    val project = ArtProject("draft", mode, sheets.toList(), frames.toList())

    AsterionTheme {
        Scaffold { insets ->
            Column(
                modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("ASTERION ART", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("References · Creation · Animation · Grading", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("01 / Reference sheets", style = MaterialTheme.typography.titleLarge)
                        Text("Character sheets are stored in Asterion Core when its integration is enabled.")
                        SheetKind.entries.forEach { kind ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = {
                                    selectedKind = kind
                                    sheetPicker.launch(arrayOf("image/*"))
                                }) { Text(if (kind == SheetKind.CHARACTER) "+ Characters" else "+ ${kind.name.replace('_', ' ')}") }
                                Text("${sheets.count { it.kind == kind }} selected")
                            }
                        }
                        sheets.forEach { sheet ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("${sheet.kind.name}: ${sheet.title}", modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = { sheets.remove(sheet) }) { Text("Remove") }
                            }
                        }
                    }
                }
                SheetAuthoringCard(onCreated = { created ->
                    if (created.kind != SheetKind.CHARACTER) sheets.removeAll { it.kind == created.kind }
                    sheets += created
                    status = "New structured " + created.kind.name.lowercase() + " sheet added."
                })
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("02 / Output mode", style = MaterialTheme.typography.titleLarge)
                        Row(modifier = Modifier.horizontalScroll(rememberHorizontalScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArtMode.entries.forEach { choice ->
                                FilterChip(selected = choice == mode, onClick = { mode = choice },
                                    label = { Text(choice.name.lowercase().replaceFirstChar { it.uppercase() }) })
                            }
                        }
                        if (mode == ArtMode.COSPLAY) {
                            Text("Fantasy anatomy is preserved: horns, wings, tails and supernatural identity features.")
                        }
                    }
                }
                if (mode == ArtMode.ANIMATION) Card {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(16.dp)) {
                        Text("03 / Storyboard keyframes", style = MaterialTheme.typography.titleLarge)
                        Text("Nightmare's Wan video model animates each starting image. " +
                            "Exact interpolation between two different endpoint images is not available in its public API.")
                        OutlinedTextField(value = motion, onValueChange = { motion = it }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Motion direction") }, minLines = 2)
                        Button(onClick = { framePicker.launch(arrayOf("image/*")) }) { Text("+ Add ordered still images") }
                        frames.forEachIndexed { index, frame ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${index + 1}. ${Uri.parse(frame.imageUri).lastPathSegment?.takeLast(25)}",
                                    modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = { frames.remove(frame) }) { Text("Remove") }
                            }
                        }
                        if (frames.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = approximate, onClick = { approximate = !approximate },
                                    label = { Text("Allow approximate segments") })
                            }
                            Text("Approximate segments do not guarantee exact arrival at the next reference frame.")
                        }
                    }
                }
                HorizontalDivider()
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
                        Text("04 / Generation backend", style = MaterialTheme.typography.titleLarge)
                        Text("Nightmare Mobile local API · 127.0.0.1:8820. API must be enabled in Nightmare settings.")
                        OutlinedTextField(value = token, onValueChange = { token = it }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nightmare API token") }, singleLine = true)
                        if (mode != ArtMode.ANIMATION) {
                            Text("Initial text-derived sheet generation. Dense visual references are not yet passed to the image model directly.")
                            Text("Candidates per batch: ${candidateCount.toInt()}")
                            Slider(value = candidateCount, onValueChange = { candidateCount = it }, valueRange = 1f..4f, steps = 2)
                            Button(
                                enabled = !running && token.isNotBlank() && project.problems().isEmpty(),
                                onClick = {
                                    running = true
                                    status = "Extracting the reference-sheet descriptions..."
                                    scope.launch {
                                        try {
                                            val draft = ArtSheetCompiler(context).compile(project)
                                            status = draft.warnings.joinToString("; ").ifBlank { "Generating..." }
                                            val bridge = NightmareBridge(context.contentResolver, context.filesDir, token.trim())
                                            val graded = mutableListOf<Pair<NightmareResult, Evidence>>()
                                            repeat(candidateCount.toInt()) { index ->
                                                status = "Generating candidate ${index + 1}/${candidateCount.toInt()}"
                                                val image = bridge.render(draft.positive, seed = 0, mode = mode)
                                                graded += image to ImageTechnicalScore.inspect(image.file)
                                            }
                                            candidates.clear()
                                            candidates.addAll(graded.map { it.first })
                                            val suggested = graded.maxByOrNull { it.second.score }
                                                ?: error("No candidate generated")
                                            latest = suggested.first
                                            technical = suggested.second
                                            status = "${graded.size} candidates generated. Highest technical-quality candidate proposed; " +
                                                "identity/style conformity has NOT been evaluated. You may select another."
                                        } catch (e: Exception) {
                                            status = "Generation failed: ${e.message}"
                                        } finally { running = false }
                                    }
                                }
                            ) { Text(if (running) "Generating..." else "Generate image from sheets") }
                            project.problems().forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                        } else {
                            Button(
                                enabled = !running && token.isNotBlank() && frames.isNotEmpty(),
                                onClick = {
                                    val planned = VideoPlanner.plan(
                                        frames.toList(),
                                        VideoCapabilities(true, false, 5),
                                        allowApproximateSequence = approximate,
                                    )
                                    when (planned) {
                                        is VideoPlanResult.Blocked -> status = planned.reason
                                        is VideoPlanResult.Ready -> {
                                            running = true
                                            status = planned.warning ?: "Running video generation..."
                                            scope.launch {
                                                try {
                                                    val bridge = NightmareBridge(
                                                        context.contentResolver, context.filesDir, token.trim(),
                                                    )
                                                    var successes = 0
                                                    val segments = mutableListOf<NightmareResult>()
                                                    for (shot in planned.shots) {
                                                        status = "Generating segment ${successes + 1}/${planned.shots.size}"
                                                        val segment = bridge.render(
                                                            prompt = shot.start.motionToNext.ifBlank { motion },
                                                            seed = 0,
                                                            image = Uri.parse(shot.start.imageUri),
                                                            requireVideo = true,
                                                            videoSeconds = shot.seconds,
                                                        )
                                                        segments += segment
                                                        successes += 1
                                                    }
                                                    latest = if (segments.size == 1) segments.first() else {
                                                        val joined = java.io.File(context.filesDir,
                                                            "results/storyboard_${UUID.randomUUID()}.mp4")
                                                        VideoAssembler.concat(segments.map { it.file }, joined)
                                                        NightmareResult(UUID.randomUUID().toString().replace("-", ""),
                                                            "video", joined, segments.first().width, segments.first().height)
                                                    }
                                                    technical = null
                                                    status = "${successes} generated MP4 segment(s), " +
                                                        "combined into one film. Strict endpoint conformity remains unverified."
                                                } catch (e: Exception) {
                                                    status = "Generation failed: ${e.message}"
                                                } finally { running = false }
                                            }
                                        }
                                    }
                                },
                            ) { Text(if (running) "Generating..." else "Generate animation") }
                        }
                        Text(status)
                        if (candidates.size > 1 && mode != ArtMode.ANIMATION) {
                            Text("Generated candidates (tap to select)")
                            candidates.forEachIndexed { index, item ->
                                OutlinedButton(onClick = {
                                    latest = item
                                    technical = ImageTechnicalScore.inspect(item.file)
                                }) { Text("Candidate ${index + 1}${if (item.file == latest?.file) " — selected" else ""}") }
                            }
                        }
                        latest?.let { video ->
                            if (video.kind == "image") {
                                val bitmap = remember(video.file.absolutePath) {
                                    BitmapFactory.decodeFile(video.file.absolutePath,
                                        BitmapFactory.Options().apply { inSampleSize = 2 })
                                }
                                bitmap?.let { picture ->
                                    Image(bitmap = picture.asImageBitmap(), contentDescription = "Generated candidate",
                                        modifier = Modifier.fillMaxWidth().height(320.dp),
                                        contentScale = ContentScale.Fit)
                                }
                            } else {
                                AndroidView(
                                    factory = { viewContext -> VideoView(viewContext).apply {
                                        setVideoPath(video.file.absolutePath)
                                        setOnPreparedListener { it.isLooping = true; start() }
                                    } },
                                    update = { player ->
                                        if (player.tag != video.file.absolutePath) {
                                            player.tag = video.file.absolutePath
                                            player.setVideoPath(video.file.absolutePath)
                                            player.start()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(240.dp),
                                )
                            }
                            Text("Saved: ${video.file.name} (${video.width}×${video.height})")
                            Text("SHA-256: ${NightmareBridge.sha256(video.file)}",
                                style = MaterialTheme.typography.bodySmall)
                            technical?.let { evidence ->
                                Text("Automated technical suggestion: ${(evidence.score * 100).toInt()}/100 " +
                                    "(confidence ${(evidence.confidence * 100).toInt()}%).")
                                Text(evidence.detail, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Your rating: ${humanRating.toInt()}/10")
                            Slider(value = humanRating, onValueChange = { humanRating = it }, valueRange = 0f..10f, steps = 9)
                            OutlinedButton(onClick = {
                                try {
                                    ArtRatingStore(context).save(video, humanRating.toInt(), mode)
                                    status = "Human rating saved in append-only Art feedback history."
                                } catch (e: Exception) { status = "Could not save rating: ${e.message}" }
                            }) { Text("Save rating") }
                            if (video.kind == "video") {
                                OutlinedButton(onClick = {
                                    running = true
                                    scope.launch {
                                        try {
                                            val urls = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                ArtExport.exportForNoWorld(context, video, sheets.map { it.id }, mode)
                                            }
                                            status = "NoWorld MP4 and unapproved asset manifest exported to Downloads/AsterionArt/NoWorld. " +
                                                "Not yet imported into NoWorld. ${urls.size} files."
                                        } catch (e: Exception) { status = "Export failed: ${e.message}" }
                                        finally { running = false }
                                    }
                                }, enabled = !running) { Text("Export to NoWorld staging") }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onOpenLegacyCompiler) { Text("Open legacy compiler (advanced)") }
                Text("Implementation branch: generation uses Nightmare's external API for now. " +
                    "Full native engine fusion, Core bridge, graded image generation and video " +
                    "continuity are still under development.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
