package com.asterion.art.engine

import android.content.Context
import org.json.JSONObject

/**
 * Reads the EXISTING owner-approved Concepts.md §4 recursive selector graph.
 * This is not a new taxonomy: BC/SF are nonselectable containers; only SC
 * leaves can be drawn. Branches, aliases, views and facets are selection pools,
 * never substitute concept identities or override the Character methodology.
 */
class CanonicalSelectorGraph private constructor(
    val roots:List<String>,
    val nodes:Map<String,Node>,
    val aliases:List<Alias>,
    val domains:Map<String,Alias>,
    val facets:List<Alias>,
    val canonicalPaths:Map<String,List<String>>
) {
    data class Node(
        val id:String,val name:String,val description:String,
        val children:List<String>,val leaves:List<String>
    )
    data class Alias(
        val terms:List<String>,val targets:List<String>,
        val kind:String="view",val description:String=""
    )
    data class Pool(val query:String,val route:String,
        val leafIds:List<String>,val sourceIds:List<String>)

    private fun key(value:String)=value.trim().lowercase()
        .replace(Regex("\\s+")," ")

    /** Recursive, deduplicated without weighting by depth or branch size. */
    private fun descendants(target:String,seen:MutableSet<String> = mutableSetOf()):
        Set<String> {
        if(target.startsWith("SC")) {
            require(target in canonicalPaths){"Unknown canonical SC: "+target}
            return setOf(target)
        }
        require(target in nodes){"Unknown canonical selector node: "+target}
        require(seen.add(target)){"Cyclic selector hierarchy at "+target}
        val node=nodes.getValue(target)
        val all=mutableSetOf<String>()
        node.leaves.forEach { leaf ->
            require(leaf in canonicalPaths)
            all+=leaf
        }
        node.children.forEach { all+=descendants(it,seen) }
        seen.remove(target)
        return all
    }
    private fun pool(query:String,route:String,targets:List<String>):Pool {
        val leaves=targets.flatMap{descendants(it)}.distinct().sorted()
        require(leaves.isNotEmpty()){"Selector pool is empty: "+query}
        return Pool(query,route,leaves,targets)
    }
    fun fullPool():Pool=pool("X","unrestricted",roots)
    fun branch(id:String):Pool=pool(id,"canonical_branch",listOf(id))

    fun broadCategories():List<Pair<Node,Int>>=roots.map { id ->
        val node=nodes.getValue(id)
        node to descendants(id).size
    }
    fun branches():List<Pair<Node,Int>>=nodes.values.sortedBy{it.name}
        .map{node->node to descendants(node.id).size}

    /**
     * Exact branch FIRST inside constrained X. Exact registered view aliases,
     * domain titles and query facets are distinct pools. An exact SC leaf is
     * reached only if there is no applicable higher-priority branch.
     * Ambiguities are reported, never silently widened.
     */
    fun resolve(query:String,leaves:Map<String,ConceptDefinition>):Pool {
        val term=key(query)
        require(term.isNotBlank()){"Empty constrained X query"}
        fun unique(options:List<Pool>,route:String):Pool? {
            if(options.isEmpty())return null
            val groups=options.distinctBy{it.leafIds}
            require(groups.size==1){
                "Ambiguous "+route+" selector '"+query+"': "+
                    groups.joinToString("; "){it.sourceIds.joinToString("+")}
            }
            return groups.single()
        }
        val exactNode=nodes.values.filter {
            key(it.name)==term || key(it.id)==term
        }.map{pool(query,"exact_branch",listOf(it.id))}
        unique(exactNode,"branch")?.let{return it}
        val alias=aliases.filter{item->
            item.kind=="view" && item.terms.any{key(it)==term}
        }.map{pool(query,"registered_view",it.targets)}
        unique(alias,"alias")?.let{return it}
        val views=domains.values.filter{entry->
            entry.terms.any{key(it)==term}
        }.map{pool(query,"domain_view",it.targets)}
        unique(views,"domain")?.let{return it}
        val facetsExact=facets.filter{entry->
            entry.terms.any{key(it)==term}
        }.map{pool(query,"facet_view",it.targets)}
        unique(facetsExact,"facet")?.let{return it}
        // Composite query facets: every meaningful word must be consumed,
        // and recognised independent constraints are intersected, not unioned.
        val facetPairs=facets.flatMap{entry->
            entry.terms.map{key(it) to entry}
        }.sortedByDescending{it.first.length}
        val matched=mutableListOf<Alias>()
        var rest=" "+term+" "
        for((phrase,facet) in facetPairs){
            val match=" "+phrase+" "
            if(rest.contains(match)){
                rest=rest.replaceFirst(match," ")
                if(facet !in matched)matched+=facet
            }
        }
        val filler=setOf("animals","animal","that","are","the",
            "and","with","of","in","a","an")
        if(matched.isNotEmpty() && rest.trim().split(Regex("\\s+"))
                .filter{it.isNotBlank()}.all{it in filler}) {
            val sets=matched.map{it.targets.flatMap{target->descendants(target)}.toSet()}
            val intersection=sets.drop(1).fold(sets.first()){acc,next->
                acc.intersect(next)
            }.sorted()
            require(intersection.isNotEmpty()){
                "Constrained wildcard has no matching canonical leaves: "+query
            }
            return Pool(query,"facet_intersection",intersection,
                matched.flatMap{it.targets}.distinct())
        }
        // Named canonical leaf is a singleton fallback, never a mechanism.
        val direct=leaves.values.filter {
            key(it.name)==term||key(it.id)==term
        }.map{pool(query,"exact_leaf",listOf(it.id))}
        unique(direct,"leaf")?.let{return it}
        val leafAliases=aliases.filter { entry ->
            entry.kind=="leaf"&&entry.terms.any{key(it)==term}
        }.map{pool(query,"leaf_alias",it.targets)}
        unique(leafAliases,"leaf alias")?.let{return it}
        error("Unknown or ambiguous canonical selector: '"+query+
            "'. Choose an exact branch, registered view or SC leaf.")
    }
    fun resolveNamedBranch(query:String):Pool? {
        val term=key(query)
        val found=nodes.values.filter{key(it.name)==term || key(it.id)==term}
        if(found.isEmpty())return null
        val unique=found.map{pool(query,"selector_family",listOf(it.id))}
            .distinctBy{it.leafIds}
        require(unique.size==1){"Ambiguous named family: "+query}
        return unique.single()
    }

    companion object {
        fun load(context:Context)=parse(context.applicationContext.assets
            .open("methodology/concepts_full_v3.md")
            .bufferedReader().use{it.readText()})
        fun parse(source:String):CanonicalSelectorGraph {
            val section=source.indexOf("## 4. Canonical recursive selector hierarchy")
            require(section>=0){"Missing owner taxonomy hierarchy"}
            val opener=source.indexOf("```json",section)
            require(opener>=0){"Missing selector JSON"}
            val begin=opener+7
            val end=source.indexOf("```",begin)
            require(end>begin)
            val j=JSONObject(source.substring(begin,end).trim())
            val roots=j.getJSONArray("roots").let{a->
                (0 until a.length()).map{a.getString(it)}
            }
            fun strings(source:org.json.JSONArray)=
                (0 until source.length()).map{source.getString(it)}
            val input=j.getJSONObject("nodes")
            val nodes=input.keys().asSequence().associateWith{id->
                val item=input.getJSONObject(id)
                Node(id,item.optString("name"),item.optString("description"),
                    strings(item.getJSONArray("children")),
                    strings(item.getJSONArray("leaves")))
            }
            fun alias(item:JSONObject)=Alias(
                strings(item.getJSONArray("terms")),
                strings(item.getJSONArray("targets")),
                item.optString("kind","view"),
                item.optString("description"))
            fun records(name:String):List<Alias> {
                val array=j.optJSONArray(name)?:return emptyList()
                return (0 until array.length()).map{alias(array.getJSONObject(it))}
            }
            val domains=j.getJSONObject("domains")
            val domain=domains.keys().asSequence().associateWith{id->
                val obj=domains.getJSONObject(id)
                Alias(listOf(obj.getString("name"),id),
                    strings(obj.getJSONArray("targets")),"domain",id)
            }
            val path=mutableMapOf<String,List<String>>()
            fun walk(id:String,ancestry:List<String>){
                require(id in nodes){"Missing selector node: "+id}
                require(id !in ancestry){"Circular selector graph"}
                val node=nodes.getValue(id)
                for(leaf in node.leaves){
                    require(Regex("SC\\d{3}").matches(leaf))
                    require(path.putIfAbsent(leaf,ancestry+id+leaf)==null) {
                        "Non-unique canonical SC path: "+leaf
                    }
                }
                node.children.forEach{walk(it,ancestry+id)}
            }
            roots.forEach{walk(it,emptyList())}
            require(path.isNotEmpty())
            require(roots.distinct().size==roots.size)
            return CanonicalSelectorGraph(roots,nodes,
                records("aliases"),domain,records("query_facets"),path)
        }
    }
}
