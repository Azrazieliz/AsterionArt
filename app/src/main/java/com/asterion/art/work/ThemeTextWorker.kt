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
import com.asterion.art.brain.BrainThinkingPolicy
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
                        .activeFor(model.id,"THEME_TEXT")?.let {engine.loadAdapter(it.absolutePath)}
                    val system="You compile exact, visual, text-only creative records. "+
                        "Follow each task-specific methodology literally. "+
                        "Return strictly valid JSON, no Markdown or preamble. "+
                        "Do not add unrelated story, explanation or character identity."
                    val effectiveSystem=BrainThinkingPolicy.system(model.id,system)
                    engine.setSystemPrompt(effectiveSystem)
                    suspend fun call(message:String,maxOutput:Int):JSONObject {
                        require(engine.countTokens(message)+
                            engine.countTokens(effectiveSystem)+maxOutput+256<capacity) {
                            "The selected text model cannot accept the full theme authority"
                        }
                        engine.restartConversation(effectiveSystem)
                        val sb=StringBuilder()
                        withTimeout(12L*60_000L) {
                            engine.sendUserPrompt(message,maxOutput).collect{sb.append(it)}
                        }
                        return cleanJson(sb.toString())
                    }
                    var current=store.load(id)
                    val sets=if(current.frozenAtomSets.size==5)current.frozenAtomSets
                        else {
                            store.stage(id,"Seeded independent roulette: five frozen atom sets")
                            show("Selecting five unrelated visual atom sets")
                            // The independent atom roulette is seeded native
                            // logic, not a costly LLM request. It is independent
                            // from keywords and B/E/T by construction.
                            val chosen=com.asterion.art.methodology
                                .AtomicThemeRoulette.generate(current.seed)
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
                        if(current.intensityV2.isNotEmpty()) {
                            append("\n")
                            append(com.asterion.art.engine.CreativeIntensity.description(
                                current.intensityV2))
                        }
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
                    // One independent frozen atom set per bounded inference.
                    // A duplicate/invalid result is retried locally without
                    // re-running already persisted source-independent themes.
                    for(index in store.load(id).draftThemes.size until 5) {
                        val atoms=sets[index]
                        show("Authoring detailed theme "+(index+1)+"/5")
                        store.stage(id,"Authoring independent theme "+(index+1)+"/5")
                        var completed=false
                        var failure="Not generated yet"
                        for(attempt in 1..3) {
                            val previous=store.load(id).draftThemes
                            val request=buildString {
                                append("AUTHOR EXACTLY ONE original TEXT-ONLY ")
                                append("visual theme, not five. This is theme ")
                                append(index+1).append(" of five independently ")
                                append("authored creations.\n")
                                append("FROZEN ATOMS: ")
                                append(atoms.joinToString(", "))
                                append("\nOWNER PARAMETERS:\n")
                                append(params)
                                append("\nMETHODOLOGY: Each original atom remains ")
                                append("explicit, unaltered and visible in the ")
                                append("fully constructed visual system. ")
                                append("Up to three atoms may combine as a heavy ")
                                append("atom. Integrate EVERY atom with the ")
                                append("owner parameters through physical ")
                                append("geometry, composition, spatial and ")
                                append("material interaction systems, not ")
                                append("camera or palette substitutions. ")
                                append("No narratives, unrelated invented ")
                                append("characters or background fillers. ")
                                append("Simple macro, rich micro, modular ")
                                append("character integration and clear ")
                                append("visual relations. Liquid optional ")
                                append("unless the owner requests it. ")
                                append("Preserve the selected character ")
                                append("configuration. No image is generated ")
                                append("in this text-only mode.\n")
                                if(previous.isNotEmpty()) {
                                    append("PREVIOUS UNIQUE THEME SYSTEMS; ")
                                    append("do not repeat their structures:\n")
                                    previous.forEach { theme ->
                                        append(theme.name).append(": ")
                                        append(theme.visualSystem.take(240))
                                        append("\n")
                                    }
                                }
                                if(attempt>1) {
                                    append("PREVIOUS OUTPUT REJECTED: ")
                                    append(failure.take(180))
                                    append(". Correct the violation while ")
                                    append("retaining every required atom.\n")
                                }
                                append("RETURN JSON ONLY with exactly two ")
                                append("nonempty string keys: ")
                                append("""{"name":"unique descriptive name","visual_system":"complete detailed physical visual system retaining all original frozen atoms"}""")
                                append("The visual_system must be a coherent ")
                                append("specific construction, at least 400 ")
                                append("characters, and clearly DIFFERENT ")
                                append("from all previous themes. ")
                                append("Do not return placeholders, repeated ")
                                append("template answers or lists of synonyms.")
                            }
                            try {
                                val item=call(request,1700)
                                val candidate=ThemeText(
                                    "T"+(index+1),item.getString("name").trim(),
                                    atoms,item.getString("visual_system").trim())
                                store.appendDraftTheme(id,candidate)
                                completed=true
                                break
                            } catch(error:Exception) {
                                if(error is kotlinx.coroutines.CancellationException)
                                    throw error
                                failure=error.message?:
                                    error.javaClass.simpleName
                                android.util.Log.w("AsterionThemeText",
                                    "Theme "+(index+1)+" attempt "+attempt+
                                        " invalid: "+failure)
                                if(attempt==3) throw IllegalArgumentException(
                                    "Theme "+(index+1)+"/5 failed validation "+
                                        "after three attempts: "+failure,error)
                            }
                        }
                        check(completed) { "Theme "+(index+1)+" was not persisted" }
                    }
                    store.finish(id,store.load(id).draftThemes)
                } finally {
                    try {
                        withContext(NonCancellable+Dispatchers.IO){engine.cleanUp()}
                    } finally {DeviceInferenceGate.release()}
                }
            }
            Result.success()
        } catch(e:Exception) {
            android.util.Log.e("AsterionThemeText",
                "Text-theme generation failed at "+
                    runCatching{store.load(id).stage}.getOrDefault("unknown"),e)
            store.fail(id,e.message?:"Text theme generation incomplete")
            Result.failure()
        }
    }
}
