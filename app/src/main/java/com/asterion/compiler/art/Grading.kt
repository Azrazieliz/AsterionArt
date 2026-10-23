package com.asterion.compiler.art

import kotlin.math.exp

enum class Criterion {
    IDENTITY, THEME, STYLE, ELEMENT, ANATOMY, TECHNICAL,
}

data class Evidence(
    val criterion: Criterion,
    val score: Double,
    /** Reliability of the checker, not the aesthetic score. */
    val confidence: Double,
    val detail: String,
) {
    init {
        require(score.isFinite() && score in 0.0..1.0)
        require(confidence.isFinite() && confidence in 0.0..1.0)
    }
}

data class Grade(
    val imageId: String,
    val evidence: List<Evidence>,
    val hardFailures: Set<String> = emptySet(),
    val humanOverall: Double? = null,
) {
    init {
        humanOverall?.let { require(it.isFinite() && it in 0.0..1.0) }
    }
}

/** Weighted candidate ranking is a preference model, NOT fine-tuning diffusion weights. */
data class RankingWeights(val values: Map<Criterion, Double>) {
    init {
        require(Criterion.entries.all { (values[it] ?: -1.0) > 0.0 })
        require(values.values.all { it.isFinite() })
    }
    fun normalized() = values.mapValues { it.value / values.values.sum() }

    companion object {
        val DEFAULT = RankingWeights(
            mapOf(Criterion.IDENTITY to 25.0, Criterion.THEME to 20.0,
                Criterion.STYLE to 15.0, Criterion.ELEMENT to 15.0,
                Criterion.ANATOMY to 15.0, Criterion.TECHNICAL to 10.0)
        )
    }
}

data class RankingResult(
    val grade: Grade,
    val suggestedScore: Double?,
    val coverage: Double,
    val eligible: Boolean,
    val humanScore: Double?,
)

/**
 * Unobserved criteria are missing evidence, not zero-quality estimates.
 * Critical constraints reject even a high-scoring image.
 */
object CandidateGrader {
    fun rank(grades: List<Grade>, weights: RankingWeights): List<RankingResult> {
        val w = weights.normalized()
        return grades.map { grade ->
            val data = grade.evidence.associateBy { it.criterion }
            val coverage = Criterion.entries.sumOf { k ->
                (data[k]?.confidence ?: 0.0) * w.getValue(k)
            }
            val estimate = if (coverage < 0.25) null else
                Criterion.entries.sumOf { k ->
                    val observation = data[k]
                    if (observation == null) 0.0 else
                        w.getValue(k) * observation.score * observation.confidence
                } / coverage
            RankingResult(
                grade = grade, suggestedScore = estimate,
                coverage = coverage,
                eligible = grade.hardFailures.isEmpty(),
                humanScore = grade.humanOverall,
            )
        }.sortedWith(compareByDescending<RankingResult> { it.eligible }
            .thenByDescending { it.humanScore ?: it.suggestedScore ?: -1.0 }
            .thenByDescending { it.coverage })
    }
}

/**
 * Sparse, bounded online preference learner.
 * Selecting one candidate over another updates ranking weights only where
 * both candidates have reliable evidence. It does not modify model binaries.
 */
class PreferenceLearner(private var weights: RankingWeights = RankingWeights.DEFAULT) {
    fun snapshot() = weights
    fun choose(preferred: Grade, rejected: Grade, learningRate: Double = 0.1): RankingWeights {
        require(learningRate.isFinite() && learningRate in 0.0..0.25)
        if (preferred.imageId == rejected.imageId) return weights
        val good = preferred.evidence.associateBy { it.criterion }
        val bad = rejected.evidence.associateBy { it.criterion }
        val current = weights.normalized()
        val updated = Criterion.entries.associateWith { kind ->
            val a = good[kind]
            val b = bad[kind]
            val old = current.getValue(kind)
            if (a == null || b == null || a.confidence < 0.5 || b.confidence < 0.5) old
            else {
                val gradient = (a.score - b.score) * a.confidence * b.confidence
                old * exp(learningRate * gradient)
            }
        }
        // Small floor prevents accidental elimination of criteria.
        weights = RankingWeights(updated.mapValues { it.value.coerceAtLeast(0.001) })
        return weights
    }
}
