package com.asterion.art.ui

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.data.EditJobStore
import com.asterion.art.work.SelectiveEditWorker
import com.asterion.art.learning.*
import com.asterion.art.methodology.CharacterPlanStore
import com.asterion.art.methodology.DetailedMethodologies
import com.asterion.art.model.*
import com.asterion.art.vision.VisionModelStore
import com.asterion.art.vision.VisionDownloadWorker
import com.asterion.art.vision.CharacterVisualAuditWorker
import com.asterion.art.vision.CharacterVisualReportStore
import com.asterion.art.vision.SemanticGradingWorker
import androidx.work.WorkManager
import androidx.work.WorkInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

/** Sole explicit grading path; design selection never updates learning. */
@Composable
fun AdvancedGradingScreen(repo:ArtRepository,
                          onEdit:(GradedAsset)->Unit,
                          onToolbox:(GradedAsset)->Unit) {
    val context=LocalContext.current
    val store=remember{DetailedGradeStore(context)}
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var message by remember{mutableStateOf<String?>(null)}
    var active by remember{mutableStateOf<GradedAsset?>(null)}
    var edited by remember{mutableStateOf<DetailedGrade?>(null)}
    var saving by remember{mutableStateOf(false)}
    val records=remember(revision){store.list()}
    val validation=remember(revision){
        GradingLearningModel(context).validation()
    }
    var visionReady by remember{mutableStateOf(
        VisionModelStore(context).installed()!=null)}
    var observationsVersion by remember{mutableIntStateOf(0)}
    var semanticWork by remember {
        mutableStateOf<Map<String,WorkInfo>>(emptyMap())
    }
    LaunchedEffect(Unit) {
        while(true) {
            delay(5000)
            visionReady=withContext(Dispatchers.IO){
                VisionModelStore(context).installed()!=null
            }
            semanticWork=withContext(Dispatchers.IO) {
                val manager=WorkManager.getInstance(context)
                repo.assets.mapNotNull{asset->
                    runCatching {
                        manager.getWorkInfosForUniqueWork(
                            SemanticGradingWorker.workName(asset.uri)).get()
                            .maxByOrNull{it.id.toString()}
                    }.getOrNull()?.let{asset.uri to it}
                }.toMap()
            }
            observationsVersion++
        }
    }
    val exportPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")){uri->
        if(uri!=null)scope.launch {
            val result=withContext(Dispatchers.IO){
                runCatching{GradingCorpusStore(context).exportZip(uri)}
            }
            result.onSuccess{
                message="Exported "+it+" detailed image/region grading examples."
            }.onFailure{message=it.message}
        }
    }
    val picker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()){uris->
        uris.forEach{uri->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onSuccess{repo.registerAsset(uri.toString())}
                .onFailure{message="Cannot retain access to the selected image"}
        }
    }
    active?.let{asset->
        key(asset.uri,edited?.id) {
            GradeEditorDialog(asset,edited,context,saving,
                onCancel={active=null;edited=null},
                onSave={draft->
                    scope.launch {
                        saving=true
                        val result=withContext(Dispatchers.IO){
                            runCatching {
                                val evidence=GradingImageEvidence(context)
                                    .capture(draft.assetUri,draft.region,draft.id)
                                val grade=draft.copy(
                                    patchPath=evidence.path,
                                    evidenceSha256=evidence.sha256,
                                    visualFeatures=evidence.features,
                                    updatedAt=System.currentTimeMillis())
                                store.save(grade)
                                GradingCorpusStore(context).export()
                                grade
                            }
                        }
                        saving=false
                        result.onSuccess {saved->
                            if(saved.targetDesignId==null)
                                saved.overall?.let{repo.setStars(saved.assetUri,it)}
                            revision++;active=null;edited=null
                            message="Detailed grading saved; local visual learning updated."
                        }.onFailure {message=it.message}
                    }
                },onDelete={id->
                    scope.launch {
                        saving=true
                        val result=withContext(Dispatchers.IO) {
                            runCatching {
                                store.delete(id)
                                GradingCorpusStore(context).export()
                            }
                        }
                        saving=false
                        result.onSuccess{
                            revision++;active=null;edited=null
                            message="Grade deleted; learned parameters rebuilt."
                        }.onFailure{message=it.message}
                    }
                })
        }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{
            Text("GRADING",color=Gold)
            Text("Detailed visual feedback",
                style=MaterialTheme.typography.headlineMedium)
            Text("Grade actual visual results and identify what to preserve or "+
                "correct, including a specific design or region. Only submitted "+
                "grading is used as learning feedback.",color=Mist)
            Button(onClick={picker.launch(arrayOf("image/*"))}) {
                Text("Import images for grading")
            }
            OutlinedButton(onClick={
                exportPicker.launch("Asterion-explicit-visual-grading.zip")
            }) { Text("Export my grading evidence for optional training") }
            Text("Export is explicit and local; no data is uploaded or trained "+
                "in the cloud automatically.",color=Mist,
                style=MaterialTheme.typography.labelSmall)
            HorizontalDivider()
            Text("LOCAL SELF-LEARNING · MEASURED",color=Gold)
            val passing=validation.count{it.validated}
            Text("Pixel-feature adaptation: "+passing+" of "+
                validation.size+" mode/dimension predictors beat the "+
                "out-of-sample baseline. This is not diffusion-model "+
                "fine-tuning or neural semantic learning.",color=Mist,
                style=MaterialTheme.typography.bodySmall)
            if(validation.isEmpty()) Text("No graded pixel evidence available yet.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
            else if(passing==0) Text(
                "Predictions remain disabled until at least 20 distinct images "+
                "per category and an actual held-out error reduction.",
                color=Mist,style=MaterialTheme.typography.bodySmall)
            validation.filter{it.validated}.take(5).forEach{metric->
                Text(metric.mode+" · "+metric.dimension+" · "+
                    metric.heldOutImages+" held-out images · MAE "+
                    "%.2f".format(metric.modelMae?:0.0)+
                    " vs baseline "+"%.2f".format(metric.baselineMae?:0.0),
                    style=MaterialTheme.typography.bodySmall,color=Gold)
            }
            if(!visionReady) {
                OutlinedButton(onClick={
                    VisionDownloadWorker.enqueue(context)
                    message="Free local Qwen3-VL installation queued "+
                        "(about 1.6 GB, one time). Progress appears in the notification."
                }) { Text("Install free offline visual intelligence") }
            } else Text("Offline Qwen3-VL image analysis installed. "+
                "Generated Character Creation sheets can be inspected "+
                "against frozen anatomy and concept specifications.",
                color=Mist,style=MaterialTheme.typography.labelSmall)
            message?.let{Text(it,color=Mist)}
        }
        if(repo.assets.isEmpty())item {Text("No images available.",color=Mist)}
        items(repo.assets.sortedByDescending{it.createdAt},key={it.uri}){asset->
            val grades=records.filter{it.assetUri==asset.uri}
            val findings=remember(asset.uri,asset.sourceJobId,observationsVersion){
                asset.sourceJobId?.let{job->
                    runCatching{CharacterVisualReportStore(context)
                        .findings(job).filter{it.sourceUri==asset.uri}
                    }.getOrNull()
                }.orEmpty()
            }
            val canAudit=remember(asset.sourceJobId){
                asset.sourceJobId?.let{job->
                    runCatching{GenerationJobStore(context).load(job)
                        .characterPlanId!=null}.getOrDefault(false)
                }?:false
            }
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    AsyncImage(model=Uri.parse(asset.uri),
                        contentDescription="Image for grading",
                        modifier=Modifier.fillMaxWidth().height(230.dp),
                        contentScale=ContentScale.Fit)
                    Text(grades.size.toString()+" detailed grade(s)",color=Gold)
                    val semantic=remember(asset.uri,observationsVersion) {
                        SemanticGradingStore(context).load(asset.uri)
                    }
                    val semanticJob=semanticWork[asset.uri]
                    if(visionReady) {
                        val pending=semanticJob!=null&&!semanticJob.state.isFinished
                        OutlinedButton(onClick={
                            if(pending)WorkManager.getInstance(context)
                                .cancelUniqueWork(SemanticGradingWorker.workName(asset.uri))
                            else SemanticGradingWorker.enqueue(
                                context,asset.uri,asset.sourceJobId)
                            message=if(pending)"Cancelling local visual assessment"
                                else "Offline Qwen3-VL assessment queued; "+
                                    "original source images will be examined."
                        },modifier=Modifier.fillMaxWidth()){
                            Text(if(pending)"Cancel semantic inspection"
                                else if(semantic!=null)"Re-analyze with Qwen3-VL"
                                else "Analyze image with Qwen3-VL")
                        }
                        if(pending)Text(
                            semanticJob?.progress?.getString("stage")
                                ?: "Offline visual analysis in progress",
                            color=Mist,style=MaterialTheme.typography.bodySmall)
                        if(semanticJob?.state==WorkInfo.State.FAILED)
                            Text("Analysis failed: "+
                                semanticJob.outputData.getString("error").orEmpty(),
                                color=Gold)
                    }
                    if(semantic!=null) {
                        HorizontalDivider()
                        Text("MACHINE SEMANTIC INSPECTION · "+
                            semantic.axes.count{it.score!=null}+
                            "/9 dimensions assessed",color=Gold)
                        Text("Qwen3-VL pixel observations; not user grades, "+
                            "not approval and not neural training. "+
                            "Source images compared: "+semantic.referenceCount,
                            color=Mist,style=MaterialTheme.typography.bodySmall)
                        semantic.axes.forEach { axis->
                            Text(axis.key.replace('_',' ')+" · "+
                                (axis.score?.let{it.toString()+"/5"}?:"Not assessable"),
                                color=White,style=MaterialTheme.typography.bodyMedium)
                            Text(axis.observation,
                                style=MaterialTheme.typography.bodySmall)
                            if(axis.correction.isNotBlank())
                                Text("Correction: "+axis.correction,color=Mist,
                                    style=MaterialTheme.typography.bodySmall)
                        }
                    }
                    if(canAudit&&visionReady){
                        OutlinedButton(onClick={
                            val job=requireNotNull(asset.sourceJobId)
                            CharacterVisualAuditWorker.enqueue(context,job)
                            message="Offline visual inspection queued; "+
                                "observations will appear below."
                        },modifier=Modifier.fillMaxWidth()){
                            Text("Inspect each character with Qwen3-VL")
                        }
                    }
                    findings.forEach{finding->
                        HorizontalDivider()
                        Text("MACHINE OBSERVATION · "+finding.characterId,
                            color=Gold)
                        Text("Unverified visual inference; not your grading.",
                            color=Mist,style=MaterialTheme.typography.labelSmall)
                        finding.conceptEvidence.forEach{(name,observed)->
                            Text(name+": "+observed,
                                style=MaterialTheme.typography.bodySmall)
                        }
                        finding.visibleFeatures.take(4).forEach{
                            Text("Visible: "+it,style=MaterialTheme.typography.bodySmall)
                        }
                        finding.uncertainty.forEach{
                            Text("Uncertain: "+it,color=Mist,
                                style=MaterialTheme.typography.bodySmall)
                        }
                        finding.correctionCandidates().take(8).forEach{
                            Text("Candidate issue: "+it,
                                style=MaterialTheme.typography.bodySmall)
                        }
                        if(finding.correctionCandidates().isNotEmpty()) {
                            OutlinedButton(onClick={
                                val index=finding.characterId
                                    .removePrefix("D").toIntOrNull()
                                val target=index?.let{number->
                                    CreativeMemoryStore(context).list().firstOrNull{
                                        it.jobId==asset.sourceJobId&&it.ordinal==number
                                    }
                                }
                                edited=DetailedGrade(
                                    assetUri=asset.uri,
                                    targetDesignId=target?.id,
                                    jobId=asset.sourceJobId,
                                    creationMode=CreationMode.CHARACTER,
                                    concepts=finding.conceptEvidence.keys.toList(),
                                    feature="Review observed issues for "+
                                        finding.characterId,
                                    correct=finding.correctionCandidates()
                                        .joinToString("\n"),
                                    preserve="")
                                active=asset
                            }) {Text("Review these issues as a grading draft")}
                        }
                    }
                    if(asset.autoScore!=null)Text(
                        "Pixel-only technical screening: "+
                            "%.0f".format(asset.autoScore)+"/100; not semantic grading",
                        color=Mist,style=MaterialTheme.typography.bodySmall)
                    grades.forEach{grade->
                        OutlinedButton(onClick={edited=grade;active=asset},
                            modifier=Modifier.fillMaxWidth()){
                            Text("Edit assessment: "+(grade.targetDesignId?:"whole image")+
                                (grade.feature.takeIf{it.isNotBlank()}
                                    ?.let{" / "+it}?: ""))
                        }
                        if(grade.correct.isNotBlank()&&asset.sourceJobId!=null &&
                            grade.region!=GradeRegion.FULL){
                            OutlinedButton(onClick={
                                runCatching{
                                    val bounds=BitmapFactory.Options().apply{
                                        inJustDecodeBounds=true
                                    }
                                    requireNotNull(context.contentResolver
                                        .openInputStream(Uri.parse(asset.uri))).use{
                                        BitmapFactory.decodeStream(it,null,bounds)
                                    }
                                    require(bounds.outWidth in 32..8192 &&
                                        bounds.outHeight in 32..8192)
                                    val mask=Bitmap.createBitmap(
                                        bounds.outWidth,bounds.outHeight,
                                        Bitmap.Config.ARGB_8888)
                                    val raw=try {
                                        val canvas=Canvas(mask)
                                        canvas.drawColor(Color.BLACK)
                                        val g=grade.region
                                        canvas.drawRect(
                                            bounds.outWidth*g.x,bounds.outHeight*g.y,
                                            bounds.outWidth*(g.x+g.width),
                                            bounds.outHeight*(g.y+g.height),
                                            Paint().apply{color=Color.WHITE})
                                        ByteArrayOutputStream().use{bytes->
                                            check(mask.compress(
                                                Bitmap.CompressFormat.PNG,100,bytes))
                                            bytes.toByteArray()
                                        }
                                    } finally{mask.recycle()}
                                    val edit=EditJobStore(context).create(
                                        asset.uri,requireNotNull(asset.sourceJobId),
                                        raw,0.35f,correction=grade.correct,
                                        preserve=grade.preserve,gradeId=grade.id)
                                    val task=repo.recordTask(edit.id,TaskKind.IMAGE_EDIT)
                                    SelectiveEditWorker.enqueue(context,edit.id,task.id)
                                }.onSuccess{
                                    message="User-approved masked refinement queued. "+
                                        "Unselected pixels are preserved."
                                }.onFailure{message=it.message}
                            }){Text("Refine only this graded image region")}
                        }
                    }
                    Button(onClick={edited=null;active=asset},
                        modifier=Modifier.fillMaxWidth()){
                        Text("New detailed grade")
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(onClick={onEdit(asset)}){Text("Edit pixels")}
                        OutlinedButton(onClick={onToolbox(asset)}){Text("Image toolbox")}
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeEditorDialog(
    asset:GradedAsset,existing:DetailedGrade?,
    context:android.content.Context,saving:Boolean,
    onCancel:()->Unit,onSave:(DetailedGrade)->Unit,
    onDelete:(String)->Unit
){
    val designs=remember(asset.uri){
        CreativeMemoryStore(context).list()
            .filter{it.sheetUri==asset.uri||it.previewUri==asset.uri}
            .sortedBy{it.ordinal}
    }
    val origin=remember(asset.sourceJobId) {
        asset.sourceJobId?.let{runCatching{
            GenerationJobStore(context).load(it)}.getOrNull()}
    }
    var designId by remember(asset.uri,existing?.id){
        mutableStateOf(existing?.targetDesignId)
    }
    val chosen=designs.firstOrNull{it.id==designId}
    val mode=chosen?.creationMode?:origin?.creationMode
    var scores by remember(asset.uri,existing?.id){
        mutableStateOf(existing?.ratings.orEmpty())
    }
    var overall by remember(asset.uri,existing?.id){
        mutableIntStateOf(existing?.overall?:0)
    }
    var feature by remember(asset.uri,existing?.id){
        mutableStateOf(existing?.feature.orEmpty())
    }
    var preserve by remember(asset.uri,existing?.id){
        mutableStateOf(existing?.preserve.orEmpty())
    }
    var correct by remember(asset.uri,existing?.id){
        mutableStateOf(existing?.correct.orEmpty())
    }
    var localized by remember(asset.uri,existing?.id){
        mutableStateOf(existing?.region!=null&&existing.region!=GradeRegion.FULL)
    }
    var x by remember(asset.uri,existing?.id){
        mutableFloatStateOf(existing?.region?.x?:0f)
    }
    var y by remember(asset.uri,existing?.id){
        mutableFloatStateOf(existing?.region?.y?:0f)
    }
    var width by remember(asset.uri,existing?.id){
        mutableFloatStateOf(existing?.region?.width?:0.25f)
    }
    var height by remember(asset.uri,existing?.id){
        mutableFloatStateOf(existing?.region?.height?:0.25f)
    }
    var error by remember{mutableStateOf<String?>(null)}
    AlertDialog(
        onDismissRequest={if(!saving)onCancel()},
        title={Text("Detailed visual grading")},
        text={
            Column(Modifier.heightIn(max=535.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(9.dp)){
                AsyncImage(model=Uri.parse(asset.uri),
                    contentDescription="Image evidence",
                    modifier=Modifier.fillMaxWidth().height(160.dp),
                    contentScale=ContentScale.Fit)
                if(designs.isNotEmpty()){
                    Text("TARGET DESIGN",color=Gold)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        item{FilterChip(selected=designId==null,
                            onClick={designId=null},label={Text("Whole image")})}
                        items(designs,key={it.id}){item->
                            FilterChip(selected=designId==item.id,
                                onClick={designId=item.id},label={
                                    Text("D"+item.ordinal.toString().padStart(3,'0'))
                                })
                        }
                    }
                }
                GradeScale("Overall (optional)",overall){overall=it}
                Text("SCOPED GRADING DIMENSIONS",color=Gold)
                Text("0 = not assessed. Scores never constitute automatic "+
                    "corrections to unrelated features.",color=Mist,
                    style=MaterialTheme.typography.labelSmall)
                GradeDimension.entries
                    .filter{it!=GradeDimension.TEMPORAL ||
                        origin?.mode==RenderMode.ANIMATION}
                    .forEach { axis->
                        GradeScale(axis.title,scores[axis]?:0){v->
                            scores=if(v==0)scores-axis else scores+(axis to v)
                        }
                    }
                OutlinedTextField(value=feature,
                    onValueChange={feature=it.take(300)},
                    label={Text("Exact feature or structure")},
                    modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Checkbox(checked=localized,onCheckedChange={localized=it})
                    Text("Target a specific rectangle of the original image",
                        style=MaterialTheme.typography.bodySmall)
                }
                if(designId!=null && !localized)
                    Text("The selected design's own sheet cell is the pixel "+
                        "evidence region by default. Select custom rectangle "+
                        "for a smaller correction area.",color=Mist,
                        style=MaterialTheme.typography.labelSmall)
                if(localized){
                    Text("Normalized original-image coordinates, not screen coordinates",
                        color=Mist,style=MaterialTheme.typography.labelSmall)
                    RegionScale("Left",x,1f-width){x=it}
                    RegionScale("Top",y,1f-height){y=it}
                    RegionScale("Width",width,1f-x){width=it}
                    RegionScale("Height",height,1f-y){height=it}
                }
                OutlinedTextField(value=preserve,
                    onValueChange={preserve=it.take(5000)},
                    label={Text("Preserve — exact desired qualities")},
                    modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=6)
                OutlinedTextField(value=correct,
                    onValueChange={correct=it.take(5000)},
                    label={Text("Correct — exact problems and desired changes")},
                    modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=6)
                Text("Saving captures pixel evidence privately. No upload. "+
                    "The current Qwen3 model reads your authored corrections, "+
                    "not the pixels directly.",color=Mist,
                    style=MaterialTheme.typography.labelSmall)
                if(existing!=null&&
                    DetailedGradeStore(context).forAsset(asset.uri)
                        .any{it.id==existing.id})
                    TextButton(enabled=!saving,
                    onClick={onDelete(existing.id)}){Text("Delete assessment")}
                error?.let{Text(it,color=Red)}
            }
        },
        confirmButton={
            TextButton(enabled=!saving,onClick={
                val conceptNames=runCatching{
                    if(mode==CreationMode.CHARACTER &&
                        origin?.characterPlanId!=null&&chosen!=null)
                        CharacterPlanStore(context).load(origin.characterPlanId)
                            .frozen.firstOrNull{it.number==chosen.ordinal}
                            ?.primary?.map{it.name}
                    else null
                }.getOrNull()?:origin?.concepts.orEmpty()
                val region=if(localized)
                    GradeRegion(x,y,width.coerceIn(0.01f,1f-x),
                        height.coerceIn(0.01f,1f-y))
                    else if(chosen!=null) {
                        val geometry=mode?.let {
                            DetailedMethodologies.all[it]?.catalogue
                        }
                        val cols=geometry?.columns ?: 1
                        val rows=geometry?.rows ?: 1
                        val perSheet=geometry?.designsPerImage ?: 1
                        val index=(chosen.ordinal-1)%perSheet
                        GradeRegion((index%cols).toFloat()/cols,
                            (index/cols).toFloat()/rows,
                            1f/cols,1f/rows)
                    } else GradeRegion.FULL
                val draft=DetailedGrade(
                    id=existing?.id?:UUID.randomUUID().toString(),
                    assetUri=asset.uri,targetDesignId=designId,
                    jobId=asset.sourceJobId,
                    creationMode=mode,renderMode=origin?.mode,
                    concepts=conceptNames,ratings=scores,
                    overall=overall.takeIf{it in 1..5},
                    feature=feature,preserve=preserve,correct=correct,
                    region=region)
                runCatching{draft.validate()}
                    .onSuccess{onSave(draft)}
                    .onFailure{error=it.message}
            }){Text(if(saving)"Saving…" else "Save grading")}
        },
        dismissButton={
            TextButton(enabled=!saving,onClick=onCancel){Text("Cancel")}
        }
    )
}

@Composable
private fun GradeScale(label:String,value:Int,onChange:(Int)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement=Arrangement.SpaceBetween){
            Text(label,style=MaterialTheme.typography.bodySmall)
            Text(if(value==0)"Unassessed" else value.toString()+"/5",
                color=Mist,style=MaterialTheme.typography.bodySmall)
        }
        Slider(value=value.toFloat(),
            onValueChange={onChange(it.roundToInt())},
            valueRange=0f..5f,steps=4)
    }
}
@Composable
private fun RegionScale(label:String,value:Float,maxValue:Float,
                        onChange:(Float)->Unit){
    val max=maxValue.coerceAtLeast(0.01f)
    Column{
        Text(label+" "+(value*100).roundToInt().toString()+"%",
            color=Mist,style=MaterialTheme.typography.labelSmall)
        Slider(value=value.coerceIn(0f,max),onValueChange=onChange,
            valueRange=0f..max)
    }
}
