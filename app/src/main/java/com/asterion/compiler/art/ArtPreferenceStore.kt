package com.asterion.compiler.art

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Saves a separate adaptive preference model for each output mode.
 * This adjusts candidate ranking without touching diffusion weights.
 * Explicit user approval is required before learning from a selection.
 */
class ArtPreferenceStore(context: Context) {
    private val dir = File(context.filesDir, "asterion/feedback").apply { mkdirs() }

    fun weights(mode: ArtMode): RankingWeights {
        val file = File(dir, mode.name.lowercase() + "_weights.json")
        if (!file.isFile) return RankingWeights.DEFAULT
        return runCatching {
            val json = JSONObject(file.readText())
            require(json.getInt("version") == 1)
            RankingWeights(Criterion.entries.associateWith {
                json.getDouble(it.name)
            })
        }.getOrElse { RankingWeights.DEFAULT }
    }

    fun learnFromChoice(
        mode: ArtMode,
        chosen: NightmareResult,
        suggested: NightmareResult,
        chosenEvidence: Evidence,
        suggestedEvidence: Evidence,
    ): RankingWeights {
        require(chosen.file.isFile && suggested.file.isFile)
        if (chosen.file == suggested.file) return weights(mode)
        val learner = PreferenceLearner(weights(mode))
        val chosenGrade = Grade(chosen.runId, listOf(chosenEvidence))
        val suggestedGrade = Grade(suggested.runId, listOf(suggestedEvidence))
        val updated = learner.choose(chosenGrade, suggestedGrade)
        val json = JSONObject().put("version", 1).put("mode", mode.name)
        updated.normalized().forEach { (criterion, score) -> json.put(criterion.name, score) }
        val file = File(dir, mode.name.lowercase() + "_weights.json")
        val temporary = File(dir, file.name + ".part")
        temporary.writeText(json.toString(2))
        check(temporary.renameTo(file)) { "Could not persist preference weights" }
        File(dir, "choices.jsonl").appendText(
            JSONObject()
                .put("mode", mode.name)
                .put("preferredSha256", NightmareBridge.sha256(chosen.file))
                .put("rejectedSha256", NightmareBridge.sha256(suggested.file))
                .put("timestamp", System.currentTimeMillis())
                .toString() + "\n",
        )
        return updated
    }
}
