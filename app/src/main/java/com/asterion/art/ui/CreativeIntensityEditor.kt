package com.asterion.art.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asterion.art.engine.CreativeIntensity

/**
 * Shared controller for the IMAGE as a whole. Every methodology receives a
 * frozen snapshot at Start. B/E/T have the same outer structure as before;
 * subaxes and extra dimensions are independently configurable.
 */
@Composable
fun CreativeIntensityEditor(
    values:Map<String,String>,
    onChange:(Map<String,String>)->Unit
) {
    var expanded by remember{mutableStateOf(false)}
    var category by remember{mutableStateOf("")}
    var level by remember{mutableStateOf("")}
    var custom by remember{mutableStateOf("")}
    var showExpanded by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("CREATIVE INTENSITY · V2",color=Gold)
        Text("0 = off · 1 = already prominent · 2 strong · 3 very strong · "+
            "4 extreme · 5 dominant · 6 overwhelming · 7 massive · "+
            "8 pervasive · 9 near maximal · 10 maximum · "+
            "values above 10 continue without an artificial ceiling.",
            color=Mist,style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("B","E","T").forEach { key ->
                val axis=CreativeIntensity.axes.first{it.key==key}
                OutlinedTextField(
                    value=values[key].orEmpty(),
                    onValueChange={candidate->
                        onChange(if(candidate.isBlank())values-key
                            else values+(key to candidate))
                    },
                    modifier=Modifier.weight(1f),
                    label={Text(axis.key)},singleLine=true,
                    supportingText={Text(axis.name.take(12))}
                )
            }
        }
        Text("B = Badassness · E = Eroticity · T = Contextual Tension. "+
            "These values define the whole output under the selected methodology.",
            color=Mist,style=MaterialTheme.typography.bodySmall)
        OutlinedButton(modifier=Modifier.fillMaxWidth(),
            onClick={showExpanded=!showExpanded}) {
            Text(if(showExpanded)"Hide additional axes" else
                "Edit subparameters and additional axes ("+
                    values.keys.count{it !in setOf("B","E","T")}+")")
        }
        if(showExpanded) {
            values.filterKeys{it !in setOf("B","E","T")}.forEach{(key,chosen)->
                val axis=CreativeIntensity.axes.firstOrNull{it.key==key}
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(chosen,{onChange(values+(key to it))},
                        modifier=Modifier.weight(1f),singleLine=true,
                        label={Text(axis?.name?:key)})
                    TextButton(onClick={onChange(values-key)}){Text("Remove")}
                }
            }
            Box {
                OutlinedButton(onClick={expanded=true}) {
                    Text(category.ifBlank{"Choose B/E/T subaxis or new parameter"})
                }
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                    CreativeIntensity.axes.filter{it.key !in setOf("B","E","T")}
                        .forEach{item->
                            DropdownMenuItem(
                                text={Text(item.name+" · "+item.key)},
                                onClick={category=item.key;expanded=false})
                        }
                }
            }
            OutlinedTextField(custom,{custom=it},
                modifier=Modifier.fillMaxWidth(),
                label={Text("Optional additional owner-named parameter")})
            OutlinedTextField(level,{level=it},
                modifier=Modifier.fillMaxWidth(),singleLine=true,
                label={Text("Intensity (1 starts prominent; no fixed maximum)")})
            Button(enabled=(category.isNotBlank()||custom.isNotBlank()) &&
                runCatching { CreativeIntensity.parse(level) }.isSuccess,
                onClick={
                    val key=custom.trim().ifBlank{category}
                    onChange(values+(key to CreativeIntensity.parse(level).toPlainString()))
                    category="";custom="";level=""
                },modifier=Modifier.fillMaxWidth()) {
                Text("Add intensity axis")
            }
        }
    }
}
