package com.asterion.art.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.data.RequestDraftStore
import com.asterion.art.methodology.ModeTextDesign
import com.asterion.art.methodology.ModeTextPlan
import com.asterion.art.methodology.ModeTextPlanStore
import com.asterion.art.methodology.ModeTextSchemas
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import com.asterion.art.model.SheetRole
import com.asterion.art.model.TaskKind
import com.asterion.art.work.GenerationWorker
import com.asterion.art.work.ModeTextWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One image's complete mode-specific design population; never a generic prompt editor. */
@Composable
fun ModeTextBoard(
    repo:ArtRepository,
    mode:CreationMode?,
    renderMode:RenderMode,
    sheetIds:List<String>,
    sourceUri:String?,
    concepts:List<String>,
    direction:String,
    exclusions:String,
    directReferences:Map<SheetRole,List<String>> = emptyMap(),
    eligible:Boolean,
    onImageQueued:()->Unit
) {
    val context=LocalContext.current
    val store=remember(context){ModeTextPlanStore(context)}
    val drafts=remember(context){RequestDraftStore(context)}
    val schema=remember(mode){ModeTextSchemas.forMode(mode)}
    val key=remember(mode){"mode-board-"+(mode?.name?:"artwork").lowercase()}
    val original=remember(key){drafts.load(key)}
    var active by remember(key){
        mutableStateOf(original.optString("plan").takeIf{it.isNotBlank()})
    }
    var plan by remember(key){mutableStateOf<ModeTextPlan?>(null)}
    var expanded by remember(key){mutableIntStateOf(1)}
    var error by remember(key){mutableStateOf<String?>(null)}
    SideEffect {
        drafts.save(key,JSONObject().put("plan",active?:""))
    }
    LaunchedEffect(active) {
        val id=active?:return@LaunchedEffect
        while(true) {
            plan=withContext(Dispatchers.IO){
                runCatching{store.load(id)}.getOrNull()
            }
            delay(1750L)
        }
    }
    fun createTextPlan() {
        runCatching {
            require(eligible){"Complete all required input references first"}
            val p=store.create(mode,renderMode,sheetIds,
                sourceUri,concepts,direction,exclusions,directReferences)
            ModeTextWorker.enqueue(context,p.id)
            active=p.id
            plan=p
        }.onSuccess{error=null}.onFailure{error=it.message}
    }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("ROUTE B · "+schema.title.uppercase(),color=Gold)
        Text("One image · "+schema.designsPerImage+
            " complete independent "+if(schema.designsPerImage==1)
                "design" else "designs",color=Mist)
        Text("Method-specific execution fields, preserved source authorities, "+
            "one output image. Text is generated and edited first; native "+
            "rendering starts only after your explicit bridge.",color=Mist)
        if(plan==null || plan?.status=="DRAFT"||plan?.status=="FAILED") {
            Button(modifier=Modifier.fillMaxWidth(),
                enabled=eligible,onClick=::createTextPlan) {
                Text("Prepare "+schema.designsPerImage+" text designs")
            }
        }
        val current=plan
        if(current!=null) {
            Text("SAVED PLAN · "+current.id.take(8)+" · "+current.status,
                color=Gold)
            Text(current.stage,color=Mist)
            if(current.status=="READY" || current.status=="AUTHORIZED") {
                current.designs.forEach{design->
                    Card(colors=CardDefaults.cardColors(containerColor=Panel)){
                        Column(Modifier.padding(12.dp),
                            verticalArrangement=Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement=Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)){
                                    Text("DESIGN "+design.ordinal+" · "+design.name,
                                        color=Gold)
                                    Text(design.fields.values.firstOrNull()
                                        .orEmpty().take(160),color=Mist,
                                        style=MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick={expanded=if(expanded==design.ordinal)
                                    0 else design.ordinal}) {
                                    Text(if(expanded==design.ordinal)"Collapse" else "Edit")
                                }
                            }
                            if(expanded==design.ordinal) {
                                ModeTextRecordEditor(
                                    current,design,store,onUpdate={updated->
                                        plan=updated;error=null
                                    })
                            }
                        }
                    }
                }
                Text("All "+current.designs.size+" records are preserved in "+
                    "one methodology-specific image specification; nothing is "+
                    "redesigned or silently selected during bridge.",color=Mist)
                Button(
                    enabled=current.status=="READY" && eligible,
                    modifier=Modifier.fillMaxWidth(),
                    onClick={
                        runCatching {
                            val p=store.load(current.id)
                            p.schema.validate(p.designs)
                            val settings=repo.settings.copy(
                                ratio=if(p.schema.imageRatio=="SOURCE" ||
                                    p.schema.imageRatio=="CONFIGURED")
                                    repo.settings.ratio else p.schema.imageRatio,
                                batch=1)
                            val job=GenerationJobStore(context).create(
                                mode=p.renderMode,sheets=p.sheetIds,
                                settings=settings,creationMode=p.mode,
                                sourceImageUri=p.sourceImageUri,
                                concepts=p.concepts,
                                designPrompt=p.rendererAuthority(),
                                designNegative=p.exclusions,
                                directReferenceUris=p.directReferences,
                                methodologyTextPlanId=p.id)
                            store.authorize(p.id,job.id)
                            val task=repo.recordTask(job.id,TaskKind.STILL_IMAGE)
                            GenerationWorker.enqueue(context,job.id,task.id)
                        }.onSuccess{onImageQueued()}
                            .onFailure{error=it.message}
                    }) {
                    Text(if(current.status=="AUTHORIZED")
                        "Already sent to native generation"
                        else "Bridge all "+schema.designsPerImage+
                            " designs → generate one image")
                }
            }
        }
        if(error!=null)Text(error.orEmpty(),color=Red)
    }
}

@Composable
private fun ModeTextRecordEditor(
    plan:ModeTextPlan,
    design:ModeTextDesign,
    store:ModeTextPlanStore,
    onUpdate:(ModeTextPlan)->Unit
) {
    val context=LocalContext.current
    val drafts=remember(context){RequestDraftStore(context)}
    val key=remember(plan.id,design.ordinal) {
        "mode-edit-"+plan.id+"-"+design.ordinal
    }
    val saved=remember(key){drafts.load(key)}
    var name by remember(key){
        mutableStateOf(saved.optString("name",design.name))
    }
    var fields by remember(key){
        mutableStateOf(plan.schema.fields.associate{field->
            field.key to saved.optString(field.key,
                design.fields[field.key].orEmpty())
        })
    }
    var error by remember(key){mutableStateOf<String?>(null)}
    SideEffect {
        drafts.save(key,JSONObject().apply{
            put("name",name)
            fields.forEach{(field,value)->put(field,value)}
        })
    }
    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text("METHODOLOGY: "+plan.schema.title,color=Mist)
        OutlinedTextField(name,{name=it},
            modifier=Modifier.fillMaxWidth(),label={Text("Independent design name")})
        plan.schema.fields.forEach{field->
            OutlinedTextField(
                value=fields[field.key].orEmpty(),
                onValueChange={value->
                    fields=fields+(field.key to value)
                },modifier=Modifier.fillMaxWidth(),
                minLines=2,maxLines=7,
                label={Text(field.title)}
            )
        }
        Button(modifier=Modifier.fillMaxWidth(),
            enabled=plan.status=="READY",
            onClick={
                runCatching {
                    store.revise(plan.id,ModeTextDesign(
                        design.ordinal,name,fields))
                    store.load(plan.id)
                }.onSuccess {updated->
                    onUpdate(updated);error=null
                }.onFailure{error=it.message}
            }){Text("Save independent design "+design.ordinal)}
        if(error!=null)Text(error.orEmpty(),color=Red)
    }
}
