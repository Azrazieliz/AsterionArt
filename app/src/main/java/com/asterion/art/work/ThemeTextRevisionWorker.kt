package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.asterion.art.brain.BrainAdapterLibrary
import com.asterion.art.brain.BrainInferenceLock
import com.asterion.art.brain.BrainModelStore
import com.asterion.art.methodology.ThemeText
import com.asterion.art.methodology.ThemeTextStore
import com.asterion.art.nativeengine.DeviceInferenceGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

/**
 * Text-only transformations of saved themes: alter, improve or fuse.
 * Uses existing owner-selected reasoning model WITHOUT tailoring its weights.
 * Original frozen stage-one themes remain unchanged and accessible.
 */
class ThemeTextRevisionWorker(context:Context,parameters:WorkerParameters):
    CoroutineWorker(context,parameters) {
    companion object {
        private const val PLAN="plan"
        private const val INPUT="parents"
        private const val ACTION="action"
        private const val DIRECTION="direction"
        fun enqueue(
            context:Context,plan:String,parents:List<String>,
            action:String,direction:String
        ) {
            require(action in listOf("ALTER","IMPROVE","FUSE"))
            require(parents.size==if(action=="FUSE")2 else 1)
            val data=Data.Builder().putString(PLAN,plan)
                .putStringArray(INPUT,parents.toTypedArray())
                .putString(ACTION,action).putString(DIRECTION,direction).build()
            val work=OneTimeWorkRequestBuilder<ThemeTextRevisionWorker>()
                .setInputData(data).addTag("asterion-theme-revision").build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "theme-revision-"+plan,ExistingWorkPolicy.APPEND_OR_REPLACE,work)
        }
    }
    private suspend fun foreground() {
        val m=applicationContext.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(NotificationChannel(
            "asterion_theme_text","Asterion Theme Writing",
            NotificationManager.IMPORTANCE_LOW))
        val note=Notification.Builder(applicationContext,"asterion_theme_text")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Asterion · text theme revision")
            .setContentText("Revising approved visual text specification")
            .setOngoing(true).build()
        val type=if(android.os.Build.VERSION.SDK_INT>=35)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        setForeground(ForegroundInfo(7384,note,type))
    }
    override suspend fun doWork():Result {
        val id=inputData.getString(PLAN) ?:return Result.failure()
        val names=inputData.getStringArray(INPUT)?.toList().orEmpty()
        val action=inputData.getString(ACTION) ?:return Result.failure()
        val direction=inputData.getString(DIRECTION).orEmpty()
        val store=ThemeTextStore(applicationContext)
        return try {
            require(action in listOf("ALTER","IMPROVE","FUSE"))
            require(names.size==if(action=="FUSE")2 else 1)
            require(direction.isNotBlank()) {
                "Alteration, improvement or fusion requires user direction"
            }
            foreground()
            BrainInferenceLock.mutex.withLock {
                val plan=store.load(id)
                val parents=names.map { ref ->
                    plan.available.firstOrNull{it.id==ref}
                        ?:error("Theme text source no longer exists: "+ref)
                }
                require(parents.map{it.id}.distinct().size==parents.size)
                val weights=BrainModelStore(applicationContext)
                val model=weights.selected()?:error("No owner-selected text model")
                val file=weights.location(model)?:error("Missing selected text weights")
                val engine=AiChat.getInferenceEngine(applicationContext)
                val ctx=currentCoroutineContext()
                DeviceInferenceGate.acquire{!ctx.isActive}
                try {
                    withTimeout(120000L) {
                        engine.state.first{
                            it is InferenceEngine.State.Initialized ||
                                it is InferenceEngine.State.ModelReady ||
                                it is InferenceEngine.State.Error
                        }
                    }
                    val capacity=weights.contextTokens()
                    engine.loadModel(file.absolutePath,capacity)
                    BrainAdapterLibrary(applicationContext).activeFor(model.id)
                        ?.let{engine.loadAdapter(it.absolutePath)}
                    val system="Asterion text-only Theme "+action+
                        ". Preserve source theme identity, frozen source atoms, "+
                        "binding owner constraints and exact user operation. "+
                        "Do not invent unrelated concepts, scenes or narrative. "+
                        "Return strictly one valid JSON object and no prose."
                    engine.setSystemPrompt(system)
                    val source=parents.joinToString("\n\n"){theme->
                        "SOURCE "+theme.id+" NAME "+theme.name+
                            "\nFROZEN ATOMS "+theme.atoms.joinToString(", ")+
                            "\nVISUAL SYSTEM "+theme.visualSystem
                    }
                    val operation=when(action) {
                        "ALTER"->"Change ONLY the explicitly directed dimensions of "+
                            "the single source while retaining unaffected invariants."
                        "IMPROVE"->"Improve coherence, precision and physically useful "+
                            "detail of the same visual concept without redesign."
                        else->"Fuse the two distinct frozen source themes into ONE "+
                            "coherent new visual theme, preserving legitimate "+
                            "source mechanisms from both with no unrelated additions."
                    }
                    val prompt=operation+"\nOWNER DIRECTION: "+direction+
                        "\nSOURCE AUTHORITY:\n"+source+
                        "\nRETURN JSON {\"name\":\"...\",\"visual_system\":\"...\"} "+
                        "with one complete coherent and precise visual theme."
                    val outputBudget=3000
                    require(engine.countTokens(prompt)+engine.countTokens(system)+
                        outputBudget+256<capacity){
                        "Full text authority exceeds the installed model context"
                    }
                    engine.restartConversation(system)
                    val response=withTimeout(10L*60_000L){
                        engine.sendUserPrompt(prompt,outputBudget).toList().joinToString("")
                    }
                    val first=response.indexOf('{')
                    val last=response.lastIndexOf('}')
                    require(first>=0&&last>first)
                    val json=JSONObject(response.substring(first,last+1))
                    val atoms=parents.flatMap{it.atoms}.distinct()
                    val revision=ThemeText(UUID.randomUUID().toString(),
                        json.getString("name").trim(),atoms,
                        json.getString("visual_system").trim(),parents.map{it.id})
                    store.addRevision(id,revision)
                } finally {
                    try {withContext(NonCancellable+Dispatchers.IO){engine.cleanUp()}}
                    finally{DeviceInferenceGate.release()}
                }
            }
            Result.success()
        } catch(e:Exception){Result.failure()}
    }
}
