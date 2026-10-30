package com.asterion.art.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.art.data.RequestDraftStore
import com.asterion.art.methodology.CharacterPhaseOne
import com.asterion.art.methodology.CharacterPhaseOnePlan
import com.asterion.art.methodology.CharacterPlanStore
import com.asterion.art.methodology.FrozenCharacter
import org.json.JSONObject

/**
 * A Route B board represents ONE complete output image, not one character.
 * Five independently authored and fully specified identities coexist in a
 * single sheet-wide rendering contract. The user can edit each full record
 * before the one-sheet bridge. This screen makes NO image generation call.
 */
@Composable
fun CharacterDesignBoard(
    plan:CharacterPhaseOnePlan,
    store:CharacterPlanStore,
    onUpdated:(CharacterPhaseOnePlan)->Unit
) {
    require(plan.sheets==1 && plan.frozen.size==CharacterPhaseOne.PER_SHEET) {
        "Route B requires exactly one complete five-character sheet"
    }
    var expanded by remember(plan.id) { mutableStateOf(plan.frozen.first().id) }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("ROUTE B · ONE OUTPUT IMAGE",color=Gold)
        Text("ONE 16:9 SHEET · FIVE COMPLETE INDEPENDENT DESIGNS",color=White)
        Text("Shared: concept-selection plan, five distinct complete identities, "+
            "original methodology, native 2D presentation and the required "+
            "front / exact profile / rear views for every character. "+
            "Expand a character to edit its individual full specification.",
            color=Mist)
        plan.frozen.forEach { identity ->
            Card(
                border=BorderStroke(1.dp,if(expanded==identity.id)Gold else Raised),
                colors=CardDefaults.cardColors(containerColor=Panel)
            ) {
                Column(Modifier.padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(identity.id+" · "+identity.primary.joinToString(" × "){it.name},
                                color=Gold)
                            Text(identity.differentiation.take(200),color=Mist)
                        }
                        TextButton(onClick={
                            expanded=if(expanded==identity.id)"" else identity.id
                        }) { Text(if(expanded==identity.id)"Collapse" else "Edit") }
                    }
                    if(expanded==identity.id) {
                        HorizontalDivider()
                        CharacterEditor(plan.id,identity,store,onUpdated)
                    }
                }
            }
        }
        Text("Only the complete and validated five-character plan is "+
            "transferred to rendering. Source concepts and views remain "+
            "independent; changes to one identity never replace the other four.",
            color=Mist)
    }
}

@Composable
private fun CharacterEditor(
    planId:String,
    identity:FrozenCharacter,
    store:CharacterPlanStore,
    onUpdated:(CharacterPhaseOnePlan)->Unit
) {
    val context=LocalContext.current
    val drafts=remember(context){RequestDraftStore(context)}
    val draftKey=remember(planId,identity.id) {
        "char-board-"+planId+"-"+identity.id.lowercase()
    }
    val initial=remember(draftKey){drafts.load(draftKey)}
    fun original(key:String,value:String)=
        if(initial.has(key))initial.optString(key,value) else value
    var anatomy by remember(draftKey){mutableStateOf(original("anatomy",identity.anatomy))}
    var face by remember(draftKey){mutableStateOf(original("face",identity.faceEyesHair))}
    var topology by remember(draftKey){mutableStateOf(original("topology",identity.topology))}
    var fusion by remember(draftKey){mutableStateOf(original("fusion",identity.fusion))}
    var materials by remember(draftKey){mutableStateOf(original("materials",identity.materials))}
    var differentiation by remember(draftKey){
        mutableStateOf(original("differentiation",identity.differentiation))
    }
    var render by remember(draftKey){
        mutableStateOf(original("render",identity.renderDescription))
    }
    var audit by remember(draftKey){
        mutableStateOf(original("audit",identity.auditEvidence))
    }
    var higher by remember(draftKey){
        mutableStateOf(original("higher",identity.higherOrder))
    }
    var pairs by remember(draftKey){
        mutableStateOf(identity.pairwise.mapIndexed { index,pair ->
            original("pair_"+index,pair.mechanism)
        })
    }
    var error by remember(draftKey){mutableStateOf<String?>(null)}
    SideEffect {
        // Persist every local edit immediately; editing does not launch Qwen
        // or touch another character's frozen renderer specification.
        drafts.save(draftKey,JSONObject().apply {
            put("anatomy",anatomy);put("face",face)
            put("topology",topology);put("fusion",fusion)
            put("materials",materials)
            put("differentiation",differentiation)
            put("render",render);put("audit",audit)
            put("higher",higher)
            pairs.forEachIndexed{index,value->put("pair_"+index,value)}
        })
    }
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Text("PRIMARY CONCEPTS (FROZEN)",color=Gold)
        Text(identity.primary.joinToString(" × "){it.name},color=Mist)
        @Composable fun field(
            label:String,value:String,onChange:(String)->Unit
        ) {
            OutlinedTextField(
                value=value,onValueChange=onChange,
                label={Text(label)},modifier=Modifier.fillMaxWidth(),
                minLines=2,maxLines=8
            )
        }
        field("Body anatomy and proportions",anatomy){anatomy=it}
        field("Face, eyes and hair",face){face=it}
        field("Structural topology and appendage roots",topology){topology=it}
        field("Concept fusion",fusion){fusion=it}
        field("Materials and palette",materials){materials=it}
        field("Distinctive identity and differentiation",differentiation){
            differentiation=it
        }
        identity.pairwise.forEachIndexed{index,pair->
            field("Shared physical structure: "+pair.first+" × "+pair.second,
                pairs.getOrElse(index){pair.mechanism}){text->
                pairs=pairs.toMutableList().also{it[index]=text}
            }
        }
        if(identity.primary.size>=3) field(
            "All-concept higher-order fusion",higher){higher=it}
        field("Complete self-contained renderer description",render){render=it}
        field("Design audit evidence",audit){audit=it}
        if(error!=null)Text(error.orEmpty(),color=Red)
        OutlinedButton(modifier=Modifier.fillMaxWidth(),onClick={
            runCatching {
                val updated=identity.copy(
                    anatomy=anatomy,faceEyesHair=face,topology=topology,
                    fusion=fusion,materials=materials,
                    differentiation=differentiation,
                    renderDescription=render,auditEvidence=audit,
                    higherOrder=higher,
                    pairwise=identity.pairwise.mapIndexed{index,pair->
                        pair.copy(mechanism=pairs[index])
                    }
                )
                store.reviseReadyCharacter(planId,updated)
            }.onSuccess{
                error=null
                onUpdated(it)
            }.onFailure{error=it.message ?: "Design is incomplete"}
        }) {Text("Save this character's complete specification")}
    }
}
