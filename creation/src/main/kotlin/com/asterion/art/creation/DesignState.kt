package com.asterion.art.creation

import kotlin.random.Random
import kotlinx.serialization.Serializable

@Serializable
enum class DesignField {
    IDENTITY, BODY, SILHOUETTE, FACE_EYES_HAIR, TOPOLOGY, CONCEPT_FUSION, MATERIALS, PALETTE,
    CLOTHING, EQUIPMENT, POSE, COMPOSITION, INTERACTION, ENVIRONMENT, CAMERA, LIGHT,
    VISUAL_STYLE, CONSTRUCTION, MOTION,
}

@Serializable
enum class EvidenceStatus { CONFIRMED, INFERRED, AUTHORED }

@Serializable
data class VisualFact(
    val value: String,
    val status: EvidenceStatus = EvidenceStatus.AUTHORED,
    val referenceIds: List<String> = emptyList(),
) {
    init {
        require(value.isNotBlank())
        require(status != EvidenceStatus.CONFIRMED || referenceIds.isNotEmpty())
    }
}

@Serializable
data class DesignSubject(
    val id: String,
    val fields: Map<DesignField, VisualFact>,
    val region: Region? = null,
) {
    init {
        require(id.isNotBlank() && fields.isNotEmpty())
    }
}

@Serializable
enum class CharacterView { TRUE_FRONT, EXACT_PROFILE, DIRECT_REAR }

@Serializable
data class DesignState(
    val subjects: List<DesignSubject>,
    val scene: Map<DesignField, VisualFact>,
    val characterViews: List<CharacterView> = emptyList(),
    val originalPresentation: OriginalPresentation? = null,
) {
    init {
        require(subjects.map { it.id }.distinct().size == subjects.size)
        require(subjects.isNotEmpty() || scene.isNotEmpty())
    }

    fun checkReferences(references: List<Reference>) {
        val sourceIds = references.map { it.asset.id }.toSet()
        val facts = scene.values + subjects.flatMap { it.fields.values }
        require(facts.all { it.referenceIds.all(sourceIds::contains) }) {
            "The design referenced a source outside this creation"
        }
    }

    fun checkCharacterBoard() {
        require(subjects.size == 1 && characterViews == CharacterView.entries)
        val requiredFields = setOf(
            DesignField.IDENTITY, DesignField.BODY, DesignField.SILHOUETTE, DesignField.FACE_EYES_HAIR,
            DesignField.TOPOLOGY, DesignField.CONCEPT_FUSION, DesignField.MATERIALS, DesignField.PALETTE,
        )
        require(subjects.single().fields.keys.containsAll(requiredFields))
        require(subjects.single().fields.keys.intersect(setOf(DesignField.CLOTHING, DesignField.EQUIPMENT)).isEmpty()) {
            "Phase 1 authoring is confined to character identity and the body"
        }
    }
}

@Serializable
data class ThemeAtom(val id: String, val word: String) {
    init {
        require(id.isNotBlank() && word.matches(Regex("[\\p{L}][\\p{L}\\p{M}]*")))
    }
}

@Serializable
data class AtomDraw(val revision: String, val seed: Long, val atoms: List<ThemeAtom>)

class ThemeAtomPool(val revision: String, private val atoms: List<ThemeAtom>) {
    init {
        require(revision.isNotBlank())
        require(atoms.map { it.id }.distinct().size == atoms.size)
        require(atoms.map { it.word.lowercase() }.distinct().size == atoms.size)
    }

    fun sample(count: Int, seed: Long): AtomDraw {
        require(count in 1..atoms.size)
        return AtomDraw(revision, seed, atoms.sortedBy { it.id }.shuffled(Random(seed)).take(count))
    }
}

@Serializable
data class HeavyAtom(val components: List<String>, val synthesis: String) {
    init {
        require(components.size in 2..3 && components.distinct().size == components.size)
        require(synthesis.isNotBlank())
    }
}

@Serializable
data class ThemeDesign(
    val id: String,
    val title: String,
    val introduction: String,
    val visualSystems: Map<DesignField, VisualFact>,
    val atomIds: List<String>,
    val keywords: List<String>,
    val cast: ThemeCast? = null,
) {
    init {
        require(id.isNotBlank() && title.isNotBlank() && introduction.isNotBlank())
        require(
            visualSystems.keys.containsAll(
                setOf(DesignField.COMPOSITION, DesignField.ENVIRONMENT, DesignField.POSE, DesignField.MATERIALS),
            ),
        )
        require(atomIds.distinct().size == atomIds.size)
    }
}

@Serializable
data class FrozenPreparation(
    val characterSlots: List<List<FrozenConcept>> = emptyList(),
    val taxonomyHash: String? = null,
    val cast: List<ThemeSubject> = emptyList(),
    val atomDraw: AtomDraw? = null,
    val heavyAtoms: List<HeavyAtom>? = null,
)

@Serializable
data class ContentArtifact(val sha256: String, val mediaType: String, val size: Long) {
    init {
        require(sha256.matches(Regex("[a-f0-9]{64}")) && size > 0)
        require(mediaType.contains('/'))
    }
}

@Serializable
enum class TensorType(val bytes: Int) { FLOAT16(2), FLOAT32(4), INT64(8) }

@Serializable
data class ConditioningTensor(
    val asset: ContentArtifact,
    val dimensions: List<Int>,
    val type: TensorType,
) {
    init {
        require(dimensions.isNotEmpty() && dimensions.all { it > 0 })
        val bytes = dimensions.fold(type.bytes.toLong()) { size, axis -> Math.multiplyExact(size, axis.toLong()) }
        require(asset.mediaType == "application/octet-stream" && asset.size == bytes)
    }
}

@Serializable
data class ModelIdentity(
    val authorSha256: String,
    val projectionSha256: String,
    val rendererSha256: String,
) {
    init {
        require(listOf(authorSha256, projectionSha256, rendererSha256).all { it.matches(Regex("[a-f0-9]{64}")) })
    }
}

@Serializable
data class ConditionedReference(
    val assetId: String,
    val role: RefRole,
    val subject: String? = null,
    val region: Region? = null,
    val priority: Int = 0,
)

@Serializable
data class ConditioningPacket(
    val model: ModelIdentity,
    val channels: Map<String, ConditioningTensor>,
    val references: List<ConditionedReference>,
) {
    init {
        require(channels.isNotEmpty() && channels.keys.all(String::isNotBlank))
    }

    fun verify(expected: ModelIdentity, required: List<Reference>) {
        require(model == expected) { "Conditioning weights do not match the generator" }
        val assigned = required.map {
            ConditionedReference(it.asset.id, it.role, it.subject, it.region, it.priority)
        }
        require(assigned.groupingBy { it }.eachCount() == references.groupingBy { it }.eachCount()) {
            "The conditioning packet lost one or more reference bindings"
        }
    }
}
