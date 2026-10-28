package com.asterion.art

import com.asterion.art.brain.CharacterOutputSchema
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CharacterOutputSchemaTest {
    private fun full():JSONObject=JSONObject().also{obj->
        for(field in CharacterOutputSchema.identityFields) {
            obj.put(field,"specific material and anatomical construction ".repeat(
                if(field=="render_description")18 else 3))
        }
    }

    @Test fun missingFaceAndShortFieldsAreIdentifiedWithoutDiscardingExistingEvidence() {
        val response=full()
        response.remove("face_eyes_hair")
        response.put("topology","short")
        assertEquals(listOf("face_eyes_hair","topology"),
            CharacterOutputSchema.missingFields(response))
        assertTrue(response.getString("anatomy").contains("construction"))
    }

    @Test fun nestedSchemaFieldsNeedActualNeuralRepair() {
        val response=full()
        response.put("face_eyes_hair",JSONObject().put("eyes","not a prose field"))
        response.put("materials",JSONArray().put("not prose"))
        assertEquals(listOf("face_eyes_hair","materials"),
            CharacterOutputSchema.missingFields(
                CharacterOutputSchema.parseObject(response.toString())))
    }

    @Test fun truncatedAndUnterminatedJsonAreRejected() {
        val truncated="""{"anatomy":"fully formed","face_eyes_hair":"""
        assertThrows(IllegalArgumentException::class.java) {
            CharacterOutputSchema.parseObject(truncated)
        }
    }

    @Test fun markdownOrEarlierThinkingCannotReplaceFinalCompleteJson() {
        val final=full()
        val earlier=JSONObject().put("analysis","not the character")
        val raw="<think>"+earlier.toString()+"</think>\n```json\n"+
            final.toString()+"\n```"
        assertEquals(final.getString("anatomy"),
            CharacterOutputSchema.parseObject(raw).getString("anatomy"))
        assertTrue(CharacterOutputSchema.missingFields(
            CharacterOutputSchema.parseObject(raw)).isEmpty())
    }

    @Test fun pairMatrixIsExhaustiveIncludingEightConcepts() {
        assertEquals(1,CharacterOutputSchema.requiredPairs(listOf("A","B")).size)
        assertEquals(3,CharacterOutputSchema.requiredPairs(listOf("A","B","C")).size)
        val all=CharacterOutputSchema.requiredPairs((1..8).map{"SC"+it})
        assertEquals(28,all.size)
        assertEquals(28,all.toSet().size)
        assertTrue(all.contains("SC1" to "SC8"))
    }

    @Test fun pairwiseLookupAcceptsReversePairButNotMissingOrShortText() {
        val mechanism="A single continuous load bearing joint transmits the mass from "+
            "both primary materials into the supporting skeletal architecture."
        val items=JSONArray()
            .put(JSONObject().put("first","B").put("second","A")
                .put("mechanism",mechanism))
            .put(JSONObject().put("first","A").put("second","C")
                .put("mechanism","short"))
        assertEquals(mechanism,
            CharacterOutputSchema.validMechanism(items,"A","B"))
        assertNull(CharacterOutputSchema.validMechanism(items,"A","C"))
        assertNull(CharacterOutputSchema.validMechanism(items,"B","C"))
        assertNull(CharacterOutputSchema.validMechanism(null,"A","B"))
    }
}
