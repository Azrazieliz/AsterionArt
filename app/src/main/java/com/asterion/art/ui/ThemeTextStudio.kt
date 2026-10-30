package com.asterion.art.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.art.brain.BrainModelStore
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.data.RequestDraftStore
import com.asterion.art.methodology.ThemeTextPlan
import com.asterion.art.methodology.ThemeTextStore
import com.asterion.art.model.RenderMode
import com.asterion.art.model.TaskKind
import com.asterion.art.work.GenerationWorker
import com.asterion.art.work.ThemeTextWorker
import com.asterion.art.work.ThemeTextRevisionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Theme Creation is a text product. Images are exclusively created by the
 * deliberate, separate approved-theme-to-Theme-Sheet action.
 */
@Composable
fun ThemeTextStudio(repo:ArtRepository,onImageQueued:()->Unit) {
    val context=LocalContext.current
    val store=remember(context){ThemeTextStore(context)}
    val drafts=remember(context){RequestDraftStore(context)}
    val draft=remember(context){drafts.load("theme-text")}
    var b by remember { mutableStateOf(draft.optString("b")) }
    var e by remember { mutableStateOf(draft.optString("e")) }
    var t by remember { mutableStateOf(draft.optString("t")) }
    var keywords by remember { mutableStateOf(draft.optString("keywords")) }
    var characterConfig by remember {mutableStateOf(draft.optString("config"))}
    var liquid by remember {mutableStateOf(draft.optString("liquid"))}
    var axisName by remember {mutableStateOf("")}
    var axisValue by remember {mutableStateOf("")}
    // Owner-named axes are an extension container, not invented semantics.
    var axes by remember {
        mutableStateOf(draft.optJSONObject("custom")?.let{obj->
            obj.keys().asSequence().associateWith{obj.getString(it)}
        }.orEmpty())
    }
    var active by remember{
        mutableStateOf(draft.optString("plan").takeIf{it.isNotBlank()})
    }
    var plan by remember{mutableStateOf<ThemeTextPlan?>(null)}
    var selected by remember {mutableStateOf("")}
    var secondary by remember {mutableStateOf("")}
    var revisionAction by remember {mutableStateOf("ALTER")}
    var revisionDirection by remember{mutableStateOf("")}
    var editedName by remember{mutableStateOf("")}
    var editedVisual by remember{mutableStateOf("")}
    var error by remember{mutableStateOf<String?>(null)}
    val history=remember(active){store.recent()}
    SideEffect {
        drafts.save("theme-text",JSONObject().apply{
            put("b",b);put("e",e);put("t",t)
            put("keywords",keywords);put("config",characterConfig)
            put("liquid",liquid);put("custom",JSONObject(axes))
            put("plan",active?:"")
        })
    }
    LaunchedEffect(active) {
        val id=active ?:return@LaunchedEffect
        while(true) {
            plan=withContext(Dispatchers.IO){
                runCatching{store.load(id)}.getOrNull()
            }
            delay(1750L)
        }
    }
    fun readScore(label:String,value:String):Int? {
        if(value.isBlank())return null
        val parsed=value.trim().toIntOrNull()
            ?:error("$label requires an integer")
        require(parsed in 0..200){
            "$label is outside the original 0–200 scoring contract"
        }
        return parsed
    }
    fun submitText() {
        runCatching {
            val created=store.create(readScore("B",b),
                readScore("E",e),readScore("T",t),
                keywords.split(',', '\n').map{it.trim()}.filter{it.isNotBlank()},
                axes,
                characterConfig.takeIf{it.isNotBlank()},
                liquid.takeIf{it.isNotBlank()},
                System.currentTimeMillis())
            ThemeTextWorker.enqueue(context,created.id)
            active=created.id;plan=created;error=null
        }.onFailure{error=it.message}
    }
    fun selectTheme(id:String) {
        selected=id
        val theme=plan?.available?.firstOrNull{it.id==id} ?:return
        editedName=theme.name
        editedVisual=theme.visualSystem
    }

    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("THEME CREATION · TEXT ONLY",color=Gold)
            Text("B/E/T · Keywords · Atomic roulette · Theme alteration / fusion",
                style=MaterialTheme.typography.titleLarge)
            Text("This studio generates no images. Your approved text theme can "+
                "later be sent to the separate Theme Sheet image-generation bridge.",
                color=Mist)
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("ORIGINAL B/E/T STRUCTURE",color=Gold)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(b,{b=it},Modifier.weight(1f),
                            label={Text("B")},singleLine=true)
                        OutlinedTextField(e,{e=it},Modifier.weight(1f),
                            label={Text("E")},singleLine=true)
                        OutlinedTextField(t,{t=it},Modifier.weight(1f),
                            label={Text("T")},singleLine=true)
                    }
                    Text("B = Badassness · E = Eroticity · T = Contextual Tension. "+
                        "Unspecified values remain unspecified.",color=Mist,
                        style=MaterialTheme.typography.bodySmall)
                    OutlinedTextField(keywords,{keywords=it},
                        modifier=Modifier.fillMaxWidth(),
                        label={Text("Binding visual keywords (comma/newline separated)")},
                        minLines=2)
                    Text("Keywords constrain synthesis; they do not determine "+
                        "roulette atom selection.",color=Mist)
                    Text("CHARACTER CONFIGURATION",color=Gold)
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        listOf("","F","FF","FM","FFF","FFM").forEach{option->
                            FilterChip(selected=characterConfig==option,
                                onClick={characterConfig=option},
                                label={Text(if(option.isBlank())"Auto" else option)})
                        }
                    }
                    OutlinedTextField(liquid,{liquid=it},
                        modifier=Modifier.fillMaxWidth(),
                        label={Text("Optional liquid system / behavior")})
                    Text("EXPANDABLE OWNER PARAMETER AXES",color=Gold)
                    axes.forEach{(name,value)->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.SpaceBetween){
                            Text("$name: $value",Modifier.weight(1f),color=Mist)
                            TextButton(onClick={axes=axes-name}){Text("Remove")}
                        }
                    }
                    OutlinedTextField(axisName,{axisName=it},
                        label={Text("Parameter name")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(axisValue,{axisValue=it},
                        label={Text("Value / exact visual meaning")},
                        modifier=Modifier.fillMaxWidth())
                    OutlinedButton(enabled=axisName.isNotBlank() &&
                        axisValue.isNotBlank(),onClick={
                        axes=axes+(axisName.trim() to axisValue.trim())
                        axisName="";axisValue=""
                    }){Text("Add owner-defined axis")}
                    Button(modifier=Modifier.fillMaxWidth(),
                        onClick={submitText}) {
                        Text("Create five independent text themes")
                    }
                }
            }
        }
        if(history.isNotEmpty())item {
            Text("PRESERVED TEXT THEME PLANS",color=Gold)
            history.take(12).forEach{old->
                TextButton(onClick={active=old.id;plan=old;selected=""}){
                    Text(old.id.take(8)+" · "+old.status+" · "+
                        old.themes.size+" text themes")
                }
            }
        }
        plan?.let{current->
            item{
                Text("TEXT PLAN · "+current.status,color=Gold)
                Text(current.stage,color=Mist)
            }
            if(current.status=="READY"||current.status=="APPROVED") {
                items(current.available,key={it.id}){candidate->
                    Card(colors=CardDefaults.cardColors(containerColor=Panel)){
                        Column(Modifier.padding(12.dp),
                            verticalArrangement=Arrangement.spacedBy(8.dp)){
                            Text(candidate.id+" · "+candidate.name,color=Gold)
                            Text("FROZEN ATOMS: "+candidate.atoms.joinToString(" × "),
                                color=Mist)
                            Text(candidate.visualSystem,color=White,
                                style=MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick={selectTheme(candidate.id)}){
                                Text("Edit / approve this text theme")
                            }
                            if(selected==candidate.id){
                                Text("TEXT REVISION OPERATIONS",color=Gold)
                                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                    listOf("ALTER","IMPROVE","FUSE").forEach{action->
                                        FilterChip(
                                            selected=revisionAction==action,
                                            onClick={revisionAction=action},
                                            label={Text(action.lowercase().replaceFirstChar{
                                                it.uppercase()
                                            })})
                                    }
                                }
                                if(revisionAction=="FUSE") {
                                    Text("Second theme source",color=Mist)
                                    current.available.filter{it.id!=candidate.id}
                                        .forEach{other->
                                            FilterChip(
                                                selected=secondary==other.id,
                                                onClick={secondary=other.id},
                                                label={Text(other.name)})
                                        }
                                }
                                OutlinedTextField(revisionDirection,
                                    {revisionDirection=it},
                                    modifier=Modifier.fillMaxWidth(),
                                    minLines=2,maxLines=6,
                                    label={Text("Exact revision / fusion direction")})
                                OutlinedButton(
                                    enabled=revisionDirection.isNotBlank() &&
                                        (revisionAction!="FUSE" ||
                                            secondary.isNotBlank()) &&
                                        current.renderJobId==null,
                                    onClick={
                                        runCatching {
                                            ThemeTextRevisionWorker.enqueue(
                                                context,current.id,
                                                if(revisionAction=="FUSE")
                                                    listOf(candidate.id,secondary)
                                                else listOf(candidate.id),
                                                revisionAction,revisionDirection)
                                        }.onSuccess{
                                            error=null
                                        }.onFailure{error=it.message}
                                    }
                                ) {
                                    Text("Generate separate "+revisionAction.lowercase()+
                                        " text version")
                                }
                                HorizontalDivider()
                                OutlinedTextField(editedName,{editedName=it},
                                    modifier=Modifier.fillMaxWidth(),
                                    label={Text("Theme name")})
                                OutlinedTextField(editedVisual,{editedVisual=it},
                                    modifier=Modifier.fillMaxWidth(),
                                    minLines=5,maxLines=18,
                                    label={Text("Complete text visual system")})
                                Button(onClick={
                                    runCatching{
                                        store.revise(current.id,candidate.copy(
                                            name=editedName,visualSystem=editedVisual))
                                    }.onSuccess{
                                        plan=store.load(current.id)
                                        error=null
                                    }.onFailure{error=it.message}
                                }){Text("Save revised text")}
                                OutlinedButton(onClick={
                                    runCatching{
                                        // Save pending fields before approval;
                                        // never approve stale text silently.
                                        store.revise(current.id,candidate.copy(
                                            name=editedName,visualSystem=editedVisual))
                                        store.approve(current.id,candidate.id)
                                    }.onSuccess {
                                        plan=store.load(current.id)
                                        error=null
                                    }.onFailure{error=it.message}
                                }){Text("Approve this theme")}
                            }
                        }
                    }
                }
                item {
                    val approved=current.approved
                    if(approved!=null){
                        Text("APPROVED · "+approved.name,color=Green)
                        Text("The text is complete. Image creation is a separate "+
                            "explicit background job.",color=Mist)
                        Button(enabled=current.renderJobId==null,
                            modifier=Modifier.fillMaxWidth(),
                            onClick={
                                runCatching {
                                    val settings=repo.settings.copy(ratio="9:16",batch=1)
                                    val job=GenerationJobStore(context).create(
                                        mode=RenderMode.ANIME,
                                        sheets=emptyList(),settings=settings,
                                        designPrompt=current.renderAuthority(),
                                        themeTextPlanId=current.id)
                                    store.recordRender(current.id,job.id)
                                    val task=repo.recordTask(job.id,TaskKind.STILL_IMAGE)
                                    GenerationWorker.enqueue(context,job.id,task.id)
                                }.onSuccess{onImageQueued()}.onFailure{error=it.message}
                            }){
                            Text(if(current.renderJobId==null)
                                "Bridge approved text → generate Theme Sheet"
                                else "Theme Sheet already queued")
                        }
                    }
                }
            }
        }
        if(error!=null)item{Text(error.orEmpty(),color=Red)}
    }
}
