package com.asterion.art.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.learning.UserPreferenceStore
import com.asterion.art.methodology.*
import com.asterion.art.model.*
import com.asterion.art.work.GenerationWorker

/** Each named Default has its own actual source, layout and phase contract. */
@Composable
fun MethodologyStudioV3(
    repo: ArtRepository,
    method: CreationMode,
    browseSheets: () -> Unit,
    browseCore: () -> Unit,
    openParameters: () -> Unit,
    onQueued: () -> Unit
) {
    val context = LocalContext.current
    val spec = DetailedMethodologies.all.getValue(method)
    val canonical = remember { ConceptCatalog.load(context) }
    val isCharacter = method == CreationMode.CHARACTER
    var selectedConcepts by remember(method) { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSheets by remember(method) { mutableStateOf<Set<String>>(emptySet()) }
    var illustration by remember(method) { mutableStateOf<String?>(null) }
    var action by remember(method) { mutableStateOf(StageAction.CATALOGUE) }
    var media by remember(method) { mutableStateOf(RenderMode.ANIME) }
    var alphabet by remember(method) { mutableStateOf("All") }
    var direction by remember(method) { mutableStateOf("DD0001") }
    var message by remember(method) { mutableStateOf<String?>(null) }
    val picturePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onSuccess { illustration = uri.toString(); message = null }
                .onFailure { message = "Unable to retain access to the source illustration" }
        }
    }
    val chosen = repo.sheets.filter { it.id in selectedSheets }
    val names = selectedConcepts.mapNotNull { id ->
        if (method == CreationMode.ALTERNATE_UNIVERSE)
            listOf("Cyberpunk","Dark fantasy","Wuxia","Post-apocalyptic","Sci-fi","Gothic",
                "Historical","Urban occult","Steampunk","High fashion")
                .mapIndexed { i, name -> SelectableConcept("AU"+i,name) }
                .firstOrNull { it.id == id }?.label
        else canonical.firstOrNull { it.id == id }?.label
    }
    val request = MethodologyRequest(method, illustration != null, chosen.map { it.role }.toSet(),
        names, direction, action)
    val errors = DetailedMethodologies.validate(request)
    val geometry = DetailedMethodologies.geometry(request)
    val options = if(method == CreationMode.ALTERNATE_UNIVERSE)
        listOf("Cyberpunk","Dark fantasy","Wuxia","Post-apocalyptic","Sci-fi","Gothic",
            "Historical","Urban occult","Steampunk","High fashion")
            .mapIndexed { i, title -> SelectableConcept("AU"+i,title) }
    else canonical
    val needsConceptPicker = method in setOf(CreationMode.CHARACTER,CreationMode.CREATURE,
        CreationMode.TRANSFORMATION,CreationMode.MUTATION,CreationMode.ALTERNATE_UNIVERSE)
    val available = options.filter { alphabet == "All" || it.label.startsWith(alphabet, true) }
    val sourceAllowed = spec.sourcePolicy != SourcePolicy.NONE
    val showSheets = spec.requiredSheets.isNotEmpty() || spec.sourcePolicy == SourcePolicy.CHARACTER_OR_IMAGE
    val eligible = errors.isEmpty() && chosen.all { it.outputUri != null }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("METHODOLOGY DEFAULT", color = Gold, style = MaterialTheme.typography.labelMedium)
            Text(spec.name, style = MaterialTheme.typography.headlineMedium)
            Text("Source: " + spec.referenceSource, color = Mist,
                style = MaterialTheme.typography.labelSmall)
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("OUTPUT CONTRACT",color=Gold)
                    Text(if(geometry.preserveOriginalFraming) "Original image dimensions and framing"
                        else geometry.ratio + "  ·  " + geometry.designsPerImage +
                            " designs per image  ·  " + geometry.imageCount + " image(s)")
                    if(geometry.frontAndRearViews) Text(
                        "Mandatory same-identity front / 90° profile / rear views",color=Mist)
                    Text(spec.rendererDirective,color=Mist,
                        style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (isCharacter) {
            item {
                Text("CHARACTER CREATION PHASE",color=Gold)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    items(listOf(StageAction.CATALOGUE,StageAction.OUTFIT,
                        StageAction.WEAPON,StageAction.ASSEMBLY)) { phase ->
                        FilterChip(selected=action==phase,onClick={action=phase},
                            label={Text(when(phase) {
                                StageAction.CATALOGUE->"Phase 1 · Body"
                                StageAction.OUTFIT->"Phase 2 · Outfit"
                                StageAction.WEAPON->"Phase 3 · Weapon"
                                StageAction.ASSEMBLY->"Phase 4 · Assembly"
                                else->phase.name
                            })})
                    }
                }
                if(action != StageAction.CATALOGUE) Text(
                    "This phase requires explicitly selected and frozen results from previous phases. " +
                        "No automatic phase progression or substitution is permitted.",
                    color=Red,style=MaterialTheme.typography.bodySmall)
            }
            item {
                Text("DESIGN DIRECTION",color=Gold)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    items(listOf("DD0001" to "Shoujo Kaisen",
                        "DD0002" to "Fate","DD0003" to "Arknights")) { item ->
                        FilterChip(selected=direction==item.first,onClick={direction=item.first},
                            label={Text(item.second)})
                    }
                }
                Text("Design Direction defines construction grammar, not an existing character identity.",
                    color=Mist,style=MaterialTheme.typography.bodySmall)
            }
        } else {
            item {
                Text("OUTPUT TYPE",color=Gold)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    items(listOf(RenderMode.ANIME,RenderMode.REALISTIC,RenderMode.COSPLAY)) { model ->
                        FilterChip(selected=media==model,onClick={media=model},
                            label={Text(model.display)})
                    }
                }
            }
            if(StageAction.SINGLE_PREVIEW in spec.commands) item {
                Text("PRESENTATION",color=Gold)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(listOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)) { item ->
                        FilterChip(selected=action==item,onClick={action=item},
                            label={Text(if(item==StageAction.CATALOGUE)"Candidate catalogue"
                                else "One selected design")})
                    }
                }
            }
        }

        if(sourceAllowed) item {
            Text(if(spec.sourcePolicy==SourcePolicy.REQUIRED) "SOURCE ILLUSTRATION · REQUIRED"
                else "SOURCE ILLUSTRATION",color=Gold)
            OutlinedButton(onClick={picturePicker.launch(arrayOf("image/*"))}) {
                Text(if(illustration==null) "Select source illustration" else "Replace source illustration")
            }
            illustration?.let { selected ->
                AsyncImage(model=Uri.parse(selected),contentDescription="Source illustration",
                    modifier=Modifier.size(160.dp),contentScale=ContentScale.Fit)
            }
        }
        if(showSheets) {
            item {
                Text("REFERENCE AUTHORITIES",color=Gold)
                if(spec.requiredSheets==setOf(SheetRole.ART_STYLE))
                    Text("Only a target Art Style Sheet and the illustration above. No theme/character sheet.",
                        color=Mist)
                else Text("Character Sheet OR source image. Preserve the source individual.",
                    color=Mist)
                OutlinedButton(onClick=browseSheets) { Text("Build a reference sheet") }
                if(spec.sourcePolicy==SourcePolicy.CHARACTER_OR_IMAGE)
                    OutlinedButton(onClick=browseCore) { Text("Browse Asterion Core") }
            }
            items(repo.sheets.filter { it.role in spec.requiredSheets ||
                (spec.sourcePolicy==SourcePolicy.CHARACTER_OR_IMAGE && it.role==SheetRole.CHARACTER) }) { item ->
                val active=item.id in selectedSheets
                OutlinedCard(onClick={selectedSheets=if(active)selectedSheets-item.id
                    else (selectedSheets-repo.sheets.filter { it.role==item.role }.map { it.id }.toSet())+item.id},
                    border=BorderStroke(1.dp,if(active)Gold else Raised)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model=Uri.parse(item.outputUri?:item.sourceUris.first()),
                            contentDescription=item.label,modifier=Modifier.size(56.dp),
                            contentScale=ContentScale.Fit)
                        Column {
                            Text(item.label,color=White)
                            Text(if(active)"SELECTED" else item.role.display,
                                color=if(active)Gold else Mist)
                        }
                    }
                }
            }
        }
        if(needsConceptPicker) {
            item {
                Text(if(isCharacter) "PRIMARY CONCEPT SLOTS · MINIMUM TWO"
                    else "CONCEPT / DESIGN DIRECTION",color=Gold)
                Text("Selected: " + names.joinToString(" × "),color=Mist)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    items(listOf("All")+('A'..'Z').map { it.toString() }) { letter ->
                        FilterChip(selected=alphabet==letter,onClick={alphabet=letter},
                            label={Text(letter)})
                    }
                }
            }
            items(available.take(75),key={it.id}) { concept ->
                val active=concept.id in selectedConcepts
                OutlinedCard(onClick={selectedConcepts=if(active)selectedConcepts-concept.id
                    else selectedConcepts+concept.id},
                    border=BorderStroke(1.dp,if(active)Gold else Raised)) {
                    Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(if(active)"✓" else "+",color=Gold)
                        Text(concept.label,Modifier.weight(1f))
                        Text(concept.id,color=Mist,style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick=openParameters) { Text("Advanced parameters") }
            if(errors.isNotEmpty()) Text(errors.joinToString(" · "),color=Red)
            if(message!=null) Text(message.orEmpty(),color=Red)
            Button(
                modifier=Modifier.fillMaxWidth(),
                enabled=eligible && (!isCharacter || action==StageAction.CATALOGUE),
                onClick={
                    runCatching {
                        val adjusted=repo.settings.copy(
                            ratio=if(geometry.preserveOriginalFraming)repo.settings.ratio else geometry.ratio,
                            batch=geometry.imageCount
                        )
                        val job=GenerationJobStore(context).create(
                            media,chosen.map { it.id },adjusted,method,illustration,names)
                        val task=repo.recordTask(job.id,TaskKind.STILL_IMAGE)
                        GenerationWorker.enqueue(context,job.id,task.id)
                    }.onSuccess { onQueued() }.onFailure { message=it.message }
                }) {
                Text(when {
                    action==StageAction.OUTFIT && isCharacter->"Phase 2 requires a frozen body"
                    !eligible->"Complete the required authorities"
                    else->"Queue " + spec.name
                })
            }
        }
        item {
            Text("Methodology stages and rendering capabilities are verified separately. " +
                "Output must be checked against the selected reference and frozen design state.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
        }
    }
}
