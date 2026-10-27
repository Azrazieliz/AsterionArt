package com.asterion.art.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.art.brain.BrainModelStore
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.methodology.CharacterPhaseOne
import com.asterion.art.methodology.CharacterSheetCompiler
import com.asterion.art.methodology.CharacterPlanStore
import com.asterion.art.methodology.CharacterPhaseOnePlan
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import com.asterion.art.model.TaskKind
import com.asterion.art.work.CharacterPreparationWorker
import com.asterion.art.work.GenerationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Dedicated Character Creation Phase 1 user route.
 * No design-direction, sub-concept, template or phase-2 controls.
 */
@Composable
fun CharacterOriginalScreen(repo:ArtRepository,onQueued:()->Unit) {
    val context=LocalContext.current
    val store=remember{CharacterPlanStore(context)}
    val catalog=remember{ConceptCatalog.load(context)}
    var slots by remember{mutableStateOf(listOf<String>())}
    var sheets by rememberSaveable{mutableIntStateOf(CharacterPhaseOne.DEFAULT_SHEETS)}
    var search by remember{mutableStateOf("")}
    var query by remember{mutableStateOf("")}
    var openLiteral by remember{mutableStateOf("")}
    var activePlanId by rememberSaveable{mutableStateOf<String?>(null)}
    var plan by remember{mutableStateOf<CharacterPhaseOnePlan?>(null)}
    var error by remember{mutableStateOf<String?>(null)}
    var review by remember{mutableStateOf(false)}
    // First launch extracts bundled GGUFs asynchronously; a remembered
    // snapshot would remain null after installation finishes.
    var model by remember(context) {
        mutableStateOf(BrainModelStore(context).selected())
    }
    LaunchedEffect(context) {
        while(true) {
            model=withContext(Dispatchers.IO) {
                BrainModelStore(context).selected()
            }
            delay(1500L)
        }
    }
    val preparing=plan?.status=="PENDING"||plan?.status=="DESIGNING"
    val locked=preparing || plan?.ready==true

    LaunchedEffect(activePlanId) {
        val id=activePlanId?:return@LaunchedEffect
        while(true) {
            val fresh=withContext(Dispatchers.IO) {
                runCatching{store.load(id)}
            }
            fresh.onSuccess{plan=it}.onFailure{error=it.message}
            if(fresh.getOrNull()?.status=="READY"||
                fresh.getOrNull()?.status=="AUTHORIZED"||
                fresh.getOrNull()?.status=="FAILED")
                break
            delay(1500L)
        }
    }
    fun requestResolution() {
        runCatching {
            CharacterPhaseOne.validateSlots(slots)
            require(model!=null) {
                "The included Qwen3 reasoning model is still preparing, or this " +
                    "build does not contain its verified offline checkpoint."
            }
            val created=store.create(slots,sheets,System.currentTimeMillis())
            activePlanId=created.id
            plan=created
            error=null
            CharacterPreparationWorker.enqueue(context,created.id)
        }.onFailure{error=it.message}
    }
    fun authorizeGeneration() {
        runCatching {
            val ready=requireNotNull(plan).also{it.assertComplete()}
            val settings=repo.settings.copy(ratio="16:9",batch=ready.sheets)
            val job=GenerationJobStore(context).create(
                RenderMode.ANIME,emptyList(),settings,
                creationMode=CreationMode.CHARACTER,
                concepts=ready.slots,
                characterPlanId=ready.id)
            store.authorize(ready.id,job.id)
            val task=repo.recordTask(job.id,TaskKind.STILL_IMAGE)
            GenerationWorker.enqueue(context,job.id,task.id)
        }.onSuccess{
            review=false
            onQueued()
        }.onFailure{error=it.message;review=false}
    }
    val options=remember(search,catalog) {
        if(search.isBlank())catalog
        else catalog.filter { it.label.contains(search,true) ||
            it.id.contains(search,true) }
    }
    if(review&&plan?.ready==true) {
        val ready=requireNotNull(plan)
        AlertDialog(
            onDismissRequest={review=false},
            title={Text("Inspect frozen character prompts")},
            text={
                Column(Modifier.heightIn(max=500.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("All "+ready.total+" characters have been designed and frozen. "+
                        "Each character will be rendered independently as a clinical "+
                        "front/side/rear reference, then five are assembled onto "+
                        "one uninterrupted white sheet. The selected native model "+
                        "may use separate verified conditioning passes. "+
                        "Visual accuracy remains unverified until inspection.",
                        color=Mist)
                    Text("SHEET COMPOSITION CONTRACT",color=Gold)
                    Text(CharacterPhaseOne.PRESENTATION,
                        style=MaterialTheme.typography.bodySmall)
                    ready.frozen.forEach{character->
                        Text("FROZEN "+character.id,color=Gold)
                        Text(CharacterSheetCompiler.individual(character),
                            style=MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton={TextButton(onClick={review=false}) {
                Text("Close inspection")
            }},
            dismissButton={TextButton(onClick={review=false}){Text("Back")}}
        )
    }
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("ORIGINALS · CHARACTER CREATION",color=Gold,
                style=MaterialTheme.typography.labelMedium)
            Text("Phase 1 — Original anatomy",style=MaterialTheme.typography.headlineMedium)
            Text("Five independently developed female characters per sheet. "+
                "Clinical, unclothed anatomical references with complete "+
                "concept-driven body construction and original 2D anime/manhwa drawing.",
                color=Mist)
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(14.dp),
                    verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("SHEET POPULATION",color=Gold)
                    Text(sheets.toString()+" sheet(s) · "+
                        (sheets*5)+" independent characters · "+
                        (sheets*15)+" full-body views")
                    Slider(
                        value=sheets.toFloat(),
                        onValueChange={if(!locked)sheets=it.roundToInt()},
                        valueRange=1f..30f,steps=28,enabled=!locked)
                    Text("All characters are designed before any image call. "+
                        "Each sheet contains five exact front/profile/rear triples. "+
                        "Larger populations require proportionally more local reasoning time.",
                        color=Mist,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("PRIMARY CONCEPT SLOTS · MINIMUM TWO",color=Gold)
            if(slots.isEmpty())Text("Choose two or more canonical concepts or wildcards.",
                color=Mist)
            slots.forEachIndexed{i,slot->
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween){
                    Text("Slot "+(i+1)+": "+slot,color=White)
                    TextButton(enabled=!locked,onClick={
                        slots=slots.filterIndexed{j,_->j!=i}
                    }){Text("Remove")}
                }
            }
        }
        item {
            Text("OPEN FIXED CONCEPT",color=Gold)
            OutlinedTextField(value=openLiteral,
                onValueChange={openLiteral=it},
                modifier=Modifier.fillMaxWidth(),singleLine=true,
                label={Text("Literal concept not in canonical taxonomy")},
                placeholder={Text("A concept to interpret literally")})
            OutlinedButton(enabled=!locked&&slots.size<8&&
                openLiteral.trim().isNotBlank(),onClick={
                slots=slots+openLiteral.trim()
                openLiteral=""
            }){Text("Add fixed literal")}
            Text("Noncanonical fixed literals retain their original wording; "+
                "they are never silently replaced or given invented SC identifiers.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
        }
        item {
            Text("WILDCARD",color=Gold)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled=!locked&&slots.size<8,onClick={
                    slots=slots+"X"
                }){Text("Add X")}
                OutlinedTextField(
                    value=query,onValueChange={query=it},
                    modifier=Modifier.weight(1f),singleLine=true,
                    label={Text("X constraint")},
                    placeholder={Text("e.g. crystal")})
                OutlinedButton(enabled=!locked&&query.isNotBlank()&&slots.size<8,
                    onClick={
                        slots=slots+"X{"+query.trim()+"}"
                        query=""
                    }){Text("Add")}
            }
            Text("X selects a random eligible canonical concept for each character. "+
                "X{query} selects a matching canonical concept. Fixed concepts stay "+
                "fixed, and every result is frozen before design.",color=Mist,
                style=MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedTextField(value=search,onValueChange={search=it},
                label={Text("Search canonical concepts")},
                placeholder={Text("Name or SC identifier")},
                modifier=Modifier.fillMaxWidth(),singleLine=true)
        }
        item {
            HorizontalDivider()
            if(error!=null)Text(error.orEmpty(),color=Red)
            when(plan?.status) {
                "PENDING","DESIGNING"->{
                    Text("DESIGNING · "+
                        (plan?.frozen?.size?:0)+"/"+(plan?.total?:0)+
                        " complete frozen anatomical identities",color=Gold)
                    LinearProgressIndicator(
                        progress={((plan?.frozen?.size?:0).toFloat()/
                            (plan?.total?:1).coerceAtLeast(1)).coerceIn(0f,1f)},
                        modifier=Modifier.fillMaxWidth())
                }
                "READY","AUTHORIZED"->{
                    Text("CHARACTER RESOLUTION COMPLETE",color=Gold)
                    Text("Full frozen roster and "+plan?.prompts?.size+
                        " self-contained sheet context(s) ready for review.",color=Mist)
                    val ready=requireNotNull(plan)
                    ready.frozen.forEach{identity->
                        Text(identity.id+" · "+
                            identity.primary.joinToString(" × "){it.name},
                            style=MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        enabled=ready.status=="READY",
                        modifier=Modifier.fillMaxWidth(),
                        onClick=::authorizeGeneration){
                        Text(if(ready.status=="AUTHORIZED")
                            "Generation already authorized"
                            else "Validate & Generate "+ready.sheets+" sheet(s)")
                    }
                    if(ready.status=="READY") {
                        OutlinedButton(
                            modifier=Modifier.fillMaxWidth(),
                            onClick={review=true}) {
                            Text("Inspect frozen design specifications")
                        }
                    }
                }
                "FAILED"->Text("Design resolution blocked: "+
                    plan?.failure.orEmpty(),color=Red)
                else->{
                    Button(modifier=Modifier.fillMaxWidth(),
                        enabled=!locked&&slots.size>=2,
                        onClick=::requestResolution){
                        Text("Resolve all characters before generation")
                    }
                }
            }
            if(plan?.status=="FAILED"||plan?.status=="READY"||
                plan?.status=="AUTHORIZED") {
                OutlinedButton(onClick={
                    plan=null;activePlanId=null;error=null
                }){Text("Start a new population")}
            }
        }
        item { Text("CANONICAL SELECTOR · "+options.size+" results",color=Gold) }
        items(options,key={it.id}){concept->
            val selected=slots.any{it.equals(concept.label,true)}
            OutlinedCard(
                onClick={
                    if(!locked && !selected && slots.size<8)slots=slots+concept.label
                },
                border=BorderStroke(1.dp,if(selected)Gold else Raised)) {
                Row(Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(if(selected)"✓" else "+",color=Gold)
                    Text(concept.label,Modifier.weight(1f))
                    Text(concept.id,color=Mist)
                }
            }
        }
    }
}
