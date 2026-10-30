package com.asterion.art.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.art.engine.CreativeTaxonomyRegistry

/** User-managed independent taxonomies; never changes the frozen SC*** registry. */
@Composable
fun CreativeTaxonomyStudio() {
    val context=LocalContext.current
    val registry=remember(context){CreativeTaxonomyRegistry(context)}
    var open by remember {mutableStateOf(false)}
    var space by remember {mutableStateOf(CreativeTaxonomyRegistry.THEME_ATOMS)}
    var category by remember {mutableStateOf("")}
    var words by remember {mutableStateOf("")}
    var revision by remember {mutableIntStateOf(0)}
    var error by remember {mutableStateOf<String?>(null)}
    val namespaces=listOf(
        CreativeTaxonomyRegistry.THEME_ATOMS,
        CreativeTaxonomyRegistry.EXTENDED_CONCEPTS,
        CreativeTaxonomyRegistry.MATERIAL_SYSTEMS,
        CreativeTaxonomyRegistry.ENVIRONMENTS,
        CreativeTaxonomyRegistry.PHYSICAL_MECHANISMS,
        CreativeTaxonomyRegistry.COMPOSITION_SYSTEMS,
        CreativeTaxonomyRegistry.MOTION_SYSTEMS,
        CreativeTaxonomyRegistry.INTERACTION_SYSTEMS
    )
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={open=!open},modifier=Modifier.fillMaxWidth()){
            Text(if(open)"Hide expandable taxonomy library" else
                "Manage separate concept and Theme atom taxonomies")
        }
        if(open) {
            Text("Existing Character Creation canonical SC concept records "+
                "remain unchanged. New terms are stored in separate namespaced "+
                "libraries with stable original content.",color=Mist,
                style=MaterialTheme.typography.bodySmall)
            var menu by remember{mutableStateOf(false)}
            Box {
                OutlinedButton(onClick={menu=true}){Text("Taxonomy: "+space)}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                    namespaces.forEach {value->
                        DropdownMenuItem(text={Text(value.replace("_"," "))},
                            onClick={space=value;menu=false;revision++})
                    }
                }
            }
            val groups=remember(space,revision){registry.entries(space)}
            Text(groups.values.sumOf{it.size}.toString()+" entries in "+
                groups.size+" categories",color=Gold)
            groups.forEach{(name,terms)->
                Text(name+" ("+terms.size+"): "+
                    terms.take(8).joinToString(", ")+
                    if(terms.size>8)" …" else "",
                    color=Mist,style=MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(category,{category=it},
                modifier=Modifier.fillMaxWidth(),
                label={Text("Category name")})
            OutlinedTextField(words,{words=it},
                modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=5,
                label={Text("Add separate one-word atoms / terms (comma separated)")})
            Button(modifier=Modifier.fillMaxWidth(),
                enabled=category.isNotBlank()&&words.isNotBlank(),
                onClick={
                    runCatching{
                        registry.extend(space,category.trim(),
                            words.split(',', '\n').map{it.trim()}
                                .filter{it.isNotBlank()})
                    }.onSuccess{
                        revision++;words="";error=null
                    }.onFailure{error=it.message}
                }){Text("Add without replacing the existing taxonomy")}
            if(error!=null)Text(error.orEmpty(),color=Red)
        }
    }
}
