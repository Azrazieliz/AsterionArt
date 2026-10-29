package com.asterion.art

import com.asterion.art.engine.ConceptDefinition
import com.asterion.art.methodology.*
import java.util.Random
import org.junit.Assert.*
import org.junit.Test

/** Phase-1 contract tests, replacing the deleted fixed-template designer. */
class DesignBlueprintEngineTest {
    private fun concept(i:Int):ConceptDefinition {
        val n="Concept "+i
        return ConceptDefinition("SC"+i.toString().padStart(3,'0'),
            n,"Anatomical core "+n,"Major body materialization "+n,
            "Shared fusion "+n,"Outfit knowledge "+n,"Weapon knowledge "+n,
            "Interpretation boundaries "+n,"Canonical full record "+n)
    }
    private val catalogue=(1..50).map(::concept)
    @Test fun variableSheetCountReallyMeansFiveCharactersPerSheet() {
        assertEquals(5,CharacterPhaseOne.count(1))
        assertEquals(15,CharacterPhaseOne.count(3))
        assertEquals(25,CharacterPhaseOne.count(5))
        assertEquals(150,CharacterPhaseOne.countStored(30))
        assertTrue(runCatching{CharacterPhaseOne.count(6)}.isFailure)
        assertEquals("D150",CharacterPhaseOne.label(150))
    }
    @Test fun wildcardIsResolvedIndependentlyFromCanonicalLeaves() {
        val result=CharacterPrimaryResolver.resolve(
            listOf("SC001","X"),3,catalogue,Random(347L))
        assertEquals(15,result.size)
        assertTrue(result.all{it.concepts[0].canonicalId=="SC001"})
        assertTrue(result.all{it.concepts[1].canonicalId!=null})
        assertTrue(result.all{it.names.distinct().size==2})
        assertTrue(result.map{it.concepts[1].canonicalId}.distinct().size>1)
    }
    @Test fun twoWildcardSlotsCannotResolveToSameConcept() {
        val r=CharacterPrimaryResolver.resolve(
            listOf("X","X"),1,catalogue,Random(1L))
        assertTrue(r.all{it.names.distinct().size==2})
    }
    @Test fun constrainedWildcardDoesNotReplaceFixedOpenLiteral() {
        val r=CharacterPrimaryResolver.resolve(
            listOf("Novel open fixed literal","X{Concept 2}"),
            1,catalogue,Random(88L))
        assertTrue(r.all{it.concepts.first().canonicalId==null})
        assertTrue(r.all{it.concepts.last().name.contains("Concept 2")})
        assertTrue(r.all{it.concepts.last().provenance=="uniform_constrained_X"})
    }
    private fun frozen(index:Int):FrozenCharacter {
        val id=CharacterPhaseOne.label(index)
        val unique="unique anatomical chassis "+id+
            ", integrated pelvis/ribcage proportions, independent mass, roots, "+
            "true front/side/rear across the same female character. "
        val a=unique.repeat(2)
        val p=listOf(PrimaryConcept("Oni","Oni",null,"fixed_temporary_literal"),
            PrimaryConcept("Cloud","Cloud",null,"fixed_temporary_literal"))
        val render=("Character "+id+
            " Oni Cloud jointly build a non-garment organic anatomy; "+
            unique+CharacterPhaseOne.DRAWING).repeat(4)
        return FrozenCharacter(index,p,a,a,a,
            "Oni Cloud share cranial load-bearing condensation pathways. "+a,
            a,a,render,a,
            pairwise=listOf(PairwisePhysicalFusion("Oni","Cloud",
                "Both Oni and Cloud produce an integrated tensile horn, " +
                "spinal condensation substrate and shared rib-rooted support system.")))
    }
    @Test fun everySheetUsesExactlyFiveSelfContainedAnatomicalDesigns() {
        val population=(1..10).map(::frozen)
        assertTrue(CharacterSheetCompiler.audit(population,2).isEmpty())
        val second=CharacterSheetCompiler.sheet(population.drop(5),2)
        assertTrue("D006" in second)
        assertTrue("D010" in second)
        assertFalse("D005" in second)
        assertTrue(CharacterPhaseOne.PRESENTATION.contains("unclothed"))
        assertTrue(CharacterPhaseOne.PRESENTATION.contains("90-degree"))
    }
    @Test fun duplicateBodyAndFusionCannotPassPopulationAudit() {
        val source=frozen(1)
        val repeated=frozen(2).copy(
            anatomy=source.anatomy,
            faceEyesHair=source.faceEyesHair,
            topology=source.topology,
            fusion=source.fusion,
            materials=source.materials)
        val all=listOf(source,repeated)+
            (3..5).map(::frozen)
        assertTrue(CharacterSheetCompiler.audit(all,1).isNotEmpty())
    }
    @Test fun noApprovalUntilEveryCharacterIsFullyFrozen() {
        val incomplete=CharacterPhaseOnePlan(
            id="12345678",createdAt=0,sheets=1,
            slots=listOf("Oni","Cloud"),seed=1,status="DESIGNING")
        assertTrue(runCatching{incomplete.assertComplete()}.isFailure)
    }
}
