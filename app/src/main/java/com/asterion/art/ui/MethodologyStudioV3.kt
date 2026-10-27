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
import com.asterion.art.engine.GenerationPlanner
import com.asterion.art.engine.SheetTextReader
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.asterion.art.learning.UserPreferenceStore
import com.asterion.art.methodology.*
import com.asterion.art.model.*
import com.asterion.art.work.GenerationWorker
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    val spec = DetailedMethodologies.all.getValue(method)
    val canonical = remember { ConceptCatalog.load(context) }
    val taxonomy=remember { com.asterion.art.engine.ConceptTaxonomy(context) }
    val isCharacter = method == CreationMode.CHARACTER
    var showMethodDetails by remember(method) { mutableStateOf(false) }
    var selectedConcepts by remember(method) { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSheets by remember(method) { mutableStateOf<Set<String>>(emptySet()) }
    var illustration by remember(method) { mutableStateOf<String?>(null) }
    var action by remember(method) { mutableStateOf(StageAction.CATALOGUE) }
    var media by remember(method) { mutableStateOf(RenderMode.ANIME) }
    var alphabet by remember(method) { mutableStateOf("All") }
    val direction: String? = null
    var message by remember(method) { mutableStateOf<String?>(null) }
    var promptReview by remember(method) { mutableStateOf<Pair<String,String>?>(null) }
    val picturePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onSuccess { illustration = uri.toString(); message = null }
                .onFailure { message = "Unable to retain access to the source illustration" }
        }
    }
    val chosen = repo.sheets.filter { it.id in selectedSheets }
    val options = remember(method) { StudioChoices.options(method,canonical) }
    var exposure by remember(method) { mutableFloatStateOf(2f) }
    var intimidation by remember(method) { mutableFloatStateOf(3f) }
    var detail by remember(method) { mutableFloatStateOf(3f) }
    val selectedNames = selectedConcepts.mapNotNull { id ->
        options.firstOrNull { it.id == id }?.label
    }
    val parameters = when(method) {
        CreationMode.OUTFIT -> listOf(
            "Garment coverage: " + listOf("fully enclosed","conservative","moderate",
                "revealing","highly revealing","minimal coverage")[exposure.toInt()],
            "Silhouette presence: " + listOf("soft","subtle","balanced",
                "strong","intimidating","overwhelming")[intimidation.toInt()],
            "Structural detailing: " + listOf("minimal","restrained","balanced",
                "intricate","highly intricate","maximal")[detail.toInt()])
        CreationMode.WEAPON -> listOf(
            "Weapon presence: " + listOf("subtle","restrained","balanced",
                "strong","intimidating","monumental")[intimidation.toInt()],
            "Construction complexity: " + listOf("minimal","restrained","balanced",
                "intricate","elaborate","maximal")[detail.toInt()])
        else -> emptyList()
    }
    val names = selectedNames + parameters
    val request = MethodologyRequest(method, illustration != null, chosen.map { it.role }.toSet(),
        names, direction, action)
    val errors = DetailedMethodologies.validate(request)
    val geometry = DetailedMethodologies.geometry(request)
    val needsConceptPicker = method in setOf(CreationMode.CHARACTER,CreationMode.CREATURE,
        CreationMode.TRANSFORMATION,CreationMode.MUTATION,CreationMode.ALTERNATE_UNIVERSE,
        CreationMode.OUTFIT,CreationMode.WEAPON)
    val available = options.filter { alphabet == "All" || it.label.startsWith(alphabet, true) }
    val sourceAllowed = spec.sourcePolicy != SourcePolicy.NONE
    val showSheets = spec.requiredSheets.isNotEmpty() ||
        spec.sourcePolicy == SourcePolicy.CHARACTER_OR_IMAGE ||
        method in setOf(CreationMode.OUTFIT,CreationMode.WEAPON,
            CreationMode.TRANSFORMATION,CreationMode.MUTATION,CreationMode.ALTERNATE_UNIVERSE)
    val eligible = errors.isEmpty() && chosen.all { it.outputUri != null }
    fun enqueueApproved() {
        runCatching {
            val adjusted=repo.settings.copy(
                ratio=if(geometry.preserveOriginalFraming)repo.settings.ratio else geometry.ratio,
                batch=geometry.imageCount)
            val job=GenerationJobStore(context).create(
                media,chosen.map { it.id },adjusted,method,illustration,names)
            val task=repo.recordTask(job.id,TaskKind.STILL_IMAGE)
            GenerationWorker.enqueue(context,job.id,task.id)
        }.onSuccess { onQueued() }.onFailure { message=it.message }
    }
    if(promptReview!=null) {
        val review=requireNotNull(promptReview)
        AlertDialog(onDismissRequest={promptReview=null},
            title={Text("Review generation prompts")},
            text={
                Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Model-dependent token capacity is checked again before inference.",
                        color=Mist,style=MaterialTheme.typography.bodySmall)
                    Text("POSITIVE",color=Gold)
                    Text(review.first,style=MaterialTheme.typography.bodySmall)
                    Text("NEGATIVE",color=Gold)
                    Text(review.second.ifBlank { "(none in this contract)" },
                        style=MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton={TextButton(onClick={promptReview=null;enqueueApproved()}) {
                Text("Approve and queue")
            }},
            dismissButton={TextButton(onClick={promptReview=null}) { Text("Back") }})
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("CREATION STUDIO", color = Gold, style = MaterialTheme.typography.labelMedium)
            Text(spec.name, style = MaterialTheme.typography.headlineMedium)
            Text("Selected workflow",color=Mist,style=MaterialTheme.typography.labelSmall)
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
                    TextButton(onClick={showMethodDetails=!showMethodDetails}) {
                        Text(if(showMethodDetails)"Hide methodology details" else "Methodology details")
                    }
                    if(showMethodDetails) Text(spec.rendererDirective,color=Mist,
                        style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!isCharacter) {
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

        if(method in setOf(CreationMode.OUTFIT,CreationMode.WEAPON)) item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("DESIGN CONTROL",color=Gold)
                    if(method==CreationMode.OUTFIT) {
                        Text("Coverage / sensual presentation: " + exposure.toInt(),color=Mist)
                        Slider(value=exposure,onValueChange={exposure=it},
                            valueRange=0f..5f,steps=4)
                    }
                    Text("Presence / intimidation: " + intimidation.toInt(),color=Mist)
                    Slider(value=intimidation,onValueChange={intimidation=it},
                        valueRange=0f..5f,steps=4)
                    Text("Structural complexity: " + detail.toInt(),color=Mist)
                    Slider(value=detail,onValueChange={detail=it},
                        valueRange=0f..5f,steps=4)
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
                else Text("Optional visual authorities. Select multiple sheets to combine " +
                    "their relevant structural, material or identity evidence.",color=Mist)
                OutlinedButton(onClick=browseSheets) { Text("Build a reference sheet") }
                if(spec.sourcePolicy==SourcePolicy.CHARACTER_OR_IMAGE)
                    OutlinedButton(onClick=browseCore) { Text("Browse Asterion Core") }
            }
            items(repo.sheets.filter { it.role in spec.requiredSheets ||
                (method!=CreationMode.STYLE_CORRECTION && method!=CreationMode.FEMINISATION) }) { item ->
                val active=item.id in selectedSheets
                OutlinedCard(onClick={selectedSheets=if(active)selectedSheets-item.id
                    else selectedSheets+item.id},
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
                    else "CONCEPTS / STRUCTURES",color=Gold)
                Text("Selected: " + names.joinToString(" × "),color=Mist)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    items(listOf("All")+('A'..'Z').map { it.toString() }) { letter ->
                        FilterChip(selected=alphabet==letter,onClick={alphabet=letter},
                            label={Text(letter)})
                    }
                }
            }
            items(available,key={it.id}) { concept ->
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
                    coroutineScope.launch {
                    runCatching {
                        if(method==CreationMode.CHARACTER)
                            (0..2).joinToString("\n\n===== SHEET =====\n\n") {
                                DesignBlueprintEngine.sheetPrompt(names,it,taxonomy)
                            } to DesignBlueprintEngine.negative()
                        else {
                            val settings=repo.settings.copy(
                                ratio=if(geometry.preserveOriginalFraming)repo.settings.ratio
                                    else geometry.ratio,batch=geometry.imageCount)
                            val recipe=if(method==CreationMode.CREATURE)
                                GenerationPlanner.compileConcepts(names,method,media,settings,taxonomy)
                            else {
                                val evidence=chosen.map { SheetTextReader.read(context,it) }
                                runCatching {
                                    GenerationPlanner.compile(evidence,media,settings,method,names,
                                        illustration!=null,taxonomy)
                                }.getOrElse {
                                    GenerationPlanner.compileVisual(evidence,media,settings,method,names,
                                        illustration!=null,taxonomy)
                                }
                            }
                            recipe.passes.joinToString("\n\n") { it.positive } to
                                recipe.passes.joinToString("\n\n") { it.negative }
                        }
                    }.onSuccess { promptReview=it }.onFailure { message=it.message }
                    }
                }) {
                Text(when {
                    !eligible->"Complete the required authorities"
                    else->"Review prompts"
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
