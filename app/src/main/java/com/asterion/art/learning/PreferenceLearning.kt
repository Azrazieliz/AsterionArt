package com.asterion.art.learning

import android.content.Context
import com.asterion.art.model.GeneratorSettings
import com.asterion.art.model.GradedAsset
import com.asterion.art.model.RenderMode
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class FeedbackExample(
    val score: Int,
    val cfg: Float,
    val steps: Int,
    val denoise: Float
)

data class LearningSuggestion(
    val settings: GeneratorSettings,
    val evidenceCount: Int,
    val policyVersion: String = "feedback-v1"
)

/**
 * Adaptive preferences, not model training. Scores are exclusively user-
 * authored; the automatic technical score is never treated as human feedback.
 */
object PreferenceLearning {
    fun candidate(base: GeneratorSettings, index: Int): GeneratorSettings {
        if (!base.automatic) return base
        val patterns = listOf(
            Triple(-2, -0.4f, -0.04f),
            Triple(0, 0f, 0f),
            Triple(3, 0.35f, 0.035f),
            Triple(2, -0.15f, 0.02f),
        )
        val (step, guidance, strength) = patterns[index % patterns.size]
        return base.copy(
            steps = (base.steps + step).coerceIn(5, 80),
            cfg = (base.cfg + guidance).coerceIn(0.5f, 14f),
            denoise = (base.denoise + strength).coerceIn(0.05f, 0.95f),
            batch = 1
        )
    }

    fun learn(base: GeneratorSettings, examples: List<FeedbackExample>): LearningSuggestion? {
        val rated = examples.filter {
            it.score in 1..5 && it.steps in 1..100 &&
                it.cfg in 0f..25f && it.denoise in 0f..1f
        }
        if (rated.size < 6) return null
        if (rated.map { Pair(it.steps,it.cfg) }.distinct().size < 2) return null
        val favored = rated.filter { it.score >= 4 }
        if (favored.size < 2) return null
        val sum = favored.sumOf { (it.score - 2).toDouble() }
        fun average(select: (FeedbackExample) -> Double): Double =
            favored.sumOf { select(it) * (it.score-2) } / sum
        val suggestedSteps = average { it.steps.toDouble() }
        val suggestedCfg = average { it.cfg.toDouble() }
        val suggestedDenoise = average { it.denoise.toDouble() }
        // Intentionally bounded to avoid unsafe feedback loops and overfitting.
        val adjusted = base.copy(
            steps = (base.steps + (suggestedSteps-base.steps).coerceIn(-4.0,4.0)).roundToInt().coerceIn(5,80),
            cfg = (base.cfg + (suggestedCfg-base.cfg).coerceIn(-0.5,0.5).toFloat()).coerceIn(0.5f,14f),
            denoise = (base.denoise +
                (suggestedDenoise-base.denoise).coerceIn(-0.05,0.05).toFloat()).coerceIn(0.05f,0.95f)
        )
        return LearningSuggestion(adjusted,rated.size)
    }
}

class UserPreferenceStore(private val context: Context) {
    fun suggestion(
        mode: RenderMode,
        base: GeneratorSettings,
        assets: List<GradedAsset>,
    ): LearningSuggestion? {
        // Only the user's NEW explicit overall Advanced Grading assessments
        // may steer sampling settings. Historical stars and technical scores
        // must not implicitly become training labels.
        val graded=DetailedGradeStore(context).list()
            .filter { it.overall!=null && it.jobId!=null &&
                it.targetDesignId==null && (it.renderMode==mode) }
        val latest=graded.groupBy{it.assetUri}.mapValues{(_,entries)->
            entries.maxByOrNull{it.updatedAt}!!
        }
        val byJob = assets.filter {
            it.sourceJobId!=null && latest[it.uri]!=null
        }.groupBy { requireNotNull(it.sourceJobId) }
        val examples = mutableListOf<FeedbackExample>()
        byJob.forEach { (jobId, items) ->
            if (!jobId.matches(Regex("[A-Za-z0-9_-]{8,90}"))) return@forEach
            val source = File(context.filesDir,
                "generation-plans/" + jobId + ".json")
            val manifest = File(context.filesDir,
                "generation-plans/" + jobId + "-results.json")
            if (!source.isFile || !manifest.isFile) return@forEach
            runCatching {
                val plan = JSONObject(source.readText())
                if (RenderMode.valueOf(plan.getString("mode")) != mode) return@runCatching
                val results = JSONObject(manifest.readText()).getJSONArray("results")
                for (i in 0 until results.length()) {
                    val candidate = results.getJSONObject(i)
                    val rating=latest[candidate.getString("uri")]
                        ?.overall ?: continue
                    if (!candidate.has("cfg") || !candidate.has("steps")) continue
                    examples += FeedbackExample(
                        rating, candidate.getDouble("cfg").toFloat(),
                        candidate.getInt("steps"),
                        candidate.optDouble("denoise",base.denoise.toDouble()).toFloat()
                    )
                }
            }
        }
        return PreferenceLearning.learn(base,examples)
    }
}
