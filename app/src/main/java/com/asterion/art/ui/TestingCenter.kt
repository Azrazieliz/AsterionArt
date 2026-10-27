package com.asterion.art.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.art.data.ArtRepository
import com.asterion.art.model.TaskStatus
import com.asterion.art.nativeengine.NativeModelLibrary
import com.asterion.art.testing.RuntimeProbe
import com.asterion.art.testing.RuntimeProbes
import com.asterion.art.work.NativeImageSmokeWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owner-operated acceptance tests. A bundled binary or installed checkpoint
 * is labeled PRESENT, not falsely described as successfully executed.
 * Originals are not invoked or modified.
 */
@Composable
fun TestingCenter(repo:ArtRepository,openModels:()->Unit,
    openSheets:()->Unit,openArtwork:()->Unit,openVideo:()->Unit,
    openGallery:()->Unit,openGrading:()->Unit,openCore:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var probes by remember { mutableStateOf<List<RuntimeProbe>>(emptyList()) }
    var models by remember {mutableStateOf(emptyList<com.asterion.art.nativeengine.InstalledQnnModel>())}
    var selectedModel by remember{mutableStateOf<String?>(null)}
    var evaluating by remember{mutableStateOf(false)}
    var sheetTesting by remember{mutableStateOf(false)}
    var sheetOutcome by remember{mutableStateOf<String?>(null)}
    var editTesting by remember{mutableStateOf(false)}
    var editOutcome by remember{mutableStateOf<String?>(null)}
    var note by remember{mutableStateOf<String?>(null)}
    suspend fun refresh() {
        val values=withContext(Dispatchers.IO) {
            RuntimeProbes.snapshot(context) to NativeModelLibrary(context).list()
        }
        probes=values.first
        models=values.second
        if(models.none{it.id==selectedModel})selectedModel=models.firstOrNull()?.id
    }
    LaunchedEffect(Unit) {
        evaluating=true
        try {refresh()} finally {evaluating=false}
        while(true) {
            delay(1800)
            repo.reload()
        }
    }
    val pending=repo.tasks.any{it.targetId.startsWith("native-smoke-") &&
        it.status in listOf(TaskStatus.RUNNING,TaskStatus.QUEUED)}
    LazyColumn(modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item {
            Text("ASTERION ART · ACCEPTANCE",color=Gold)
            Text("Runtime testing",style=MaterialTheme.typography.headlineLarge)
            Text("Tests below execute real local code. A prerequisite marked PRESENT "+
                "does not mean a neural generation succeeded. No Original is changed.",
                color=Mist)
            OutlinedButton(enabled=!evaluating,onClick={
                scope.launch {
                    evaluating=true
                    try{refresh()}catch(t:Exception){note=t.message}
                    finally{evaluating=false}
                }
            }){Text(if(evaluating)"Checking…" else "Refresh actual runtime status")}
        }
        items(probes.size){index->
            val report=probes[index]
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.fillMaxWidth().padding(13.dp),
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(report.component,color=White)
                    Text(report.state,color=when(report.state){
                        "PRESENT"->Green;"FAILED","MISSING"->Red;else->Gold
                    })
                    Text(report.detail,color=Mist)
                }
            }
        }
        item {
            Text("REFERENCE SHEET COMPOSITION",color=Gold)
            Button(enabled=!sheetTesting,onClick={
                scope.launch {
                    sheetTesting=true;sheetOutcome=null
                    try {
                        sheetOutcome=withContext(Dispatchers.IO){
                            RuntimeProbes.sheetRoundTrip(context)
                        }
                    }catch(t:Throwable){
                        sheetOutcome="FAIL: "+(t.message?:t.javaClass.simpleName)
                    }finally{sheetTesting=false}
                }
            }){Text(if(sheetTesting)"Testing four roles…" else
                "Test four sheet types and Gallery output")}
            sheetOutcome?.let{Text(it,color=if(it.startsWith("PASS"))Green else Red)}
            Text("Uses temporary local test images, not AI drawings; cleans up "+
                "test media. Your reference library and grades are unchanged.",color=Mist)
            OutlinedButton(onClick=openSheets){Text("Open Sheet Creation")}
        }
        item {
            Text("EDITING AND IMAGE TOOLBOX",color=Gold)
            Button(enabled=!editTesting,onClick={
                scope.launch {
                    editTesting=true;editOutcome=null
                    try {
                        editOutcome=withContext(Dispatchers.IO) {
                            RuntimeProbes.editAndToolboxRoundTrip(context)
                        }
                    }catch(t:Throwable) {
                        editOutcome="FAIL: "+(t.message?:t.javaClass.simpleName)
                    }finally{editTesting=false}
                }
            }){Text(if(editTesting)"Testing image editing…" else
                "Test masked editing and image enhancements")}
            editOutcome?.let{Text(it,color=if(it.startsWith("PASS"))Green else Red)}
            OutlinedButton(onClick=openGallery){Text("Open Gallery and editing tools")}
            OutlinedButton(onClick=openGrading){Text("Open Advanced Grading")}
            OutlinedButton(onClick=openCore){Text("Open Asterion Core integration")}
        }
        item {
            Text("REAL IMAGE INFERENCE TEST",color=Gold)
            Text("Choose the model you installed. This creates one actual QNN "+
                "image using the in-app native engine, then saves it to your gallery.",
                color=Mist)
            if(models.isEmpty()) {
                Text("No converted QNN image model installed.",color=Red)
            } else {
                models.forEach{model->
                    FilterChip(selected=selectedModel==model.id,
                        onClick={selectedModel=model.id},
                        label={Text(model.label+" · "+model.backend)})
                }
            }
            Button(enabled=selectedModel!=null&&!pending,onClick={
                val modelId=selectedModel?:return@Button
                note=runCatching{
                    val task=NativeImageSmokeWorker.enqueue(context,modelId)
                    "Native image smoke test queued: "+task+
                        ". Watch the job below for actual PASS/FAIL evidence."
                }.getOrElse{it.message?:it.javaClass.simpleName}
            }) {Text(if(pending)"Inference job running…" else "Run real native image test")}
            OutlinedButton(onClick=openModels){Text("Install / select local AI models")}
            OutlinedButton(onClick=openArtwork){Text("Open Artwork Creation")}
        }
        val tasks=repo.tasks.filter{it.targetId.startsWith("native-smoke-")}
            .sortedByDescending{it.createdAt}.take(4)
        items(tasks.size) {index->
            val task=tasks[index]
            Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(task.status.name,color=if(task.status==TaskStatus.COMPLETED)
                        Green else if(task.status==TaskStatus.FAILED)Red else Gold)
                    Text(task.stage?:"Waiting for local native engine",color=Mist)
                    task.error?.let{Text(it,color=Red)}
                }
            }
        }
        item {
            Text("VIDEO ENGINE",color=Gold)
            Text("There is no independently integrated Wan native inference "+
                "engine in this build. A local Nightmare service remains "+
                "a separate dependency. This screen will not fake video output.",
                color=Mist)
            OutlinedButton(onClick=openVideo){Text("Open Video Creation")}
            note?.let{Text(it,color=Mist)}
        }
    }
}
