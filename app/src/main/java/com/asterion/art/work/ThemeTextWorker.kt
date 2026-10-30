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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

/**
 * TEXT ONLY Theme Creation. Existing local reasoning runtime, unmodified weights.
 * Roulette cannot see keywords or B/E/T. Once frozen, the five exact atom
 * sets are immutable input to molecular synthesis. No image model runs here.
 */
class ThemeTextWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        private const val ID="text_plan_id"
        private const val CHANNEL="asterion_theme_text"
        fun enqueue(context:Context,id:String) {
            val work=OneTimeWorkRequestBuilder<ThemeTextWorker>()
                .setInputData(Data.Builder().putString(ID,id).build())
                .addTag("asterion-theme-text").build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "theme-text-"+id,ExistingWorkPolicy.KEEP,work)
        }
    }

    private suspend fun show(stage:String) {
        val manager=applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL,"Asterion theme writing",NotificationManager.IMPORTANCE_LOW))
        val notice=Notification.Builder(applicationContext,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Asterion · theme text")
            .setContentText(stage.take(120))
            .setOnlyAlertOnce(true).setOngoing(true).build()
        val type=if(android.os.Build.VERSION.SDK_INT>=35)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        setForeground(ForegroundInfo(7382,notice,type))
    }

    private fun cleanJson(text:String):JSONObject {
        val start=text.indexOf('{')
        val end=text.lastIndexOf('}')
        require(start>=0&&end>start){"Model did not return complete structured theme data"}
        return JSONObject(text.substring(start,end+1))
    }

    override suspend fun doWork():Result {
        val id=inputData.getString(ID)?:return Result.failure()
        val store=ThemeTextStore(applicationContext)
        return try {
            show("Preparing textual atomic roulette")
            store.begin(id)
            BrainInferenceLock.mutex.withLock {
                val selected=BrainModelStore(applicationContext)
                val model=selected.selected() ?:error(
                    "Install and select a local reasoning model before theme creation")
                val file=selected.location(model)?:error("Selected model weights unavailable")
                val engine=AiChat.getInferenceEngine(applicationContext)
                val ctx=currentCoroutineContext()
                DeviceInferenceGate.acquire{!ctx.isActive}
                try {
                    withTimeout(120_000L) {
                        engine.state.first { it is InferenceEngine.State.Initialized ||
                            it is InferenceEngine.State.ModelReady ||
                            it is InferenceEngine.State.Error }
                    }
                    val capacity=selected.contextTokens()
                    show("Loading selected text model")
                    engine.loadModel(file.absolutePath,capacity)
                    BrainAdapterLibrary(applicationContext)
                        .activeFor(model.id)?.let {engine.loadAdapter(it.absolutePath)}
                    val system="You compile exact, visual, text-only creative records. "+
                        "Follow each task-specific methodology literally. "+
                        "Return strictly valid JSON, no Markdown or preamble. "+
                        "Do not add unrelated story, explanation or character identity."
                    engine.setSystemPrompt(system)
                    suspend fun call(message:String,maxOutput:Int):JSONObject {
                        require(engine.countTokens(message)+
                            engine.countTokens(system)+maxOutput+256<capacity) {
                            "The selected text model cannot accept the full theme authority"
                        }
                        engine.restartConversation(system)
                        val sb=StringBuilder()
                        withTimeout(12L*60_000L) {
                            engine.sendUserPrompt(message,maxOutput).collect{sb.append(it)}
                        }
                        return cleanJson(sb.toString())
                    }
                    var current=store.load(id)
                    val sets=if(current.frozenAtomSets.size==5)current.frozenAtomSets
                        else {
                            store.stage(id,"Independent roulette: selecting five atom sets")
                            show("Selecting five unrelated visual atom sets")
                            // Sampling is performed by a seeded independent
                            // taxonomy roulette, never by inference guided by
                            // the user's B/E/T or keyword values.
                            val chosen=com.asterion.art.engine.CreativeTaxonomyRegistry(
                                applicationContext).themeRoulette(current.seed)
                            store.freezeAtoms(id,chosen)
                            chosen
                        }
                    current=store.load(id)
                    store.stage(id,"Combining frozen atom sets with owner parameters")
                    show("Writing five independent text-only themes")
                    val params=buildString {
                        append("B/E/T (missing values stay unassigned): ")
                        append(listOf(current.badassness,current.eroticity,current.tension)
                            .joinToString("/"){it?.toString()?:"UNSPECIFIED"})
                        append("\nB = Badassness, E = Eroticity, T = Contextual Tension.")
                        append("\nBinding keywords: ")
                        append(current.keywords.joinToString(" | "))
                        current.characterConfiguration?.let {
                            append("\nCharacter configuration: ").append(it)
                        }
                        current.liquidDirection?.let {
                            append("\nLiquid direction: ").append(it)
                        }
                        current.extraParameters.forEach{(name,value)->
                            append("\nOwner-defined ").append(name).append(": ")
                                .append(value)
                        }
                    }
                    val request=buildString {
                        append("Produce exactly FIVE genuinely DIFFERENT TEXT-ONLY ")
                        append("visual theme systems from the five frozen atomic sets. ")
                        append("Each set must retain every original word; no rerolls, ")
                        append("new atoms, semantic substitutes, or keyword-driven ")
                        append("atom changes. Up to three atoms may form a heavy ")
                        append("atom; molecular synthesis integrates all constraints ")
                        append("into visible physical, spatial, material and interaction ")
                        append("systems without inventing narratives. ")
                        append("The five themes must differ structurally, not just ")
                        append("in camera, palette or background. Preserve character ")
                        append("modularity, visual relationships, simple macro / rich ")
                        append("micro, and the specified character configuration. ")
                        append("LIQUID IS OPTIONAL unless directed. ")
                        append("No illustrations or visual preview instructions. ")
                        append("Do not attach or request any image. ")
                        append("OWNER PARAMETERS:\n").append(params)
                        append("\nFROZEN ROULETTE SETS:\n")
                        sets.forEachIndexed{index,atoms->
                            append("THEME ").append(index+1).append(": ")
                            append(atoms.joinToString(", ")).append("\n")
                        }
                        append("Return exactly JSON {\"themes\":[")
                        append("{\"name\":\"...\",\"visual_system\":\"...\"}, ")
                        append("... total five entries]}. Names must be distinct. ")
                        append("Each visual_system must be a detailed complete ")
                        append("physically implementable text theme, not a short label.")
                    }
                    val outcome=call(request,6000).getJSONArray("themes")
                    require(outcome.length()==5){"Incomplete set of five themes"}
                    val generated=(0 until 5).map { index ->
                        val item=outcome.getJSONObject(index)
                        ThemeText("T"+(index+1),item.getString("name").trim(),
                            sets[index],item.getString("visual_system").trim())
                    }
                    val checked=current.copy(themes=generated)
                    checked.requireFive()
                    store.finish(id,generated)
                } finally {
                    try {
                        withContext(NonCancellable+Dispatchers.IO){engine.cleanUp()}
                    } finally {DeviceInferenceGate.release()}
                }
            }
            Result.success()
        } catch(e:Exception) {
            store.fail(id,e.message?:"Text theme generation incomplete")
            Result.failure()
        }
    }
}
