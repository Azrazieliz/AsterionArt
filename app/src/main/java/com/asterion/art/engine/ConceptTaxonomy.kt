package com.asterion.art.engine

import android.content.Context

data class ConceptDefinition(
    val id:String,
    val name:String,
    val core:String,
    val materialization:String,
    val symbiosis:String,
    val outfit:String,
    val weapon:String,
    val interpretationBoundary:String,
    val fullRecord:String
) {
    private fun phrase(value:String, max:Int):String {
        val clean=value.trim().replace(Regex("\\s+")," ")
            .replace(Regex("^\\*\\*[^*]+\\*\\*\\.\\s*"),"")
        val end=clean.indexOf(". ")
        val first=if(end in 12 until max) clean.substring(0,end+1)
            else clean.take(max)
        return first.trim().trimEnd(',', ';')
    }
    fun physicalInstruction(domain:CreativeDomain):String {
        val applicable=when(domain) {
            CreativeDomain.OUTFIT -> outfit.ifBlank { materialization }
            CreativeDomain.WEAPON -> weapon.ifBlank { materialization }
            else -> materialization
        }
        return name + ": " + phrase(core,205) + "; " +
            phrase(applicable,280) + "; fusion: " + phrase(symbiosis,175) +
            "; interpretation guard: " + phrase(interpretationBoundary,155)
    }
}

interface ConceptResolver {
    fun find(name:String):ConceptDefinition?
}

class ConceptTaxonomy(context:Context):ConceptResolver {
    private val entries:Map<String,ConceptDefinition> = synchronized(ConceptTaxonomy) {
        cached ?: parse(context.assets.open("methodology/concepts_full_v3.md")
            .bufferedReader().use { it.readText() }).also { cached=it }
    }
    private val byName=entries.values.associateBy { it.name.lowercase() }
    override fun find(name:String):ConceptDefinition? =
        entries[name.trim().uppercase()] ?: byName[name.trim().lowercase()]
    fun list():List<ConceptDefinition> = entries.values.toList()
    companion object {
        @Volatile private var cached:Map<String,ConceptDefinition>? = null
        private val heading=Regex("(?m)^### (SC\\d{3}) — ([^\\r\\n]+)")
        private val section=Regex("(?m)^#### ([^\\r\\n]+)")
        fun parse(source:String):Map<String,ConceptDefinition> {
            val records=heading.findAll(source).toList()
            val sixth=source.indexOf("## 6. VARIANT / SUBCONCEPT REGISTRY")
                .let { if(it<0)source.length else it }
            return records.mapIndexedNotNull { index,match ->
                val id=match.groupValues[1]
                val name=match.groupValues[2].trim()
                val end=minOf(records.getOrNull(index+1)?.range?.first ?: source.length,sixth)
                if(end<=match.range.first) return@mapIndexedNotNull null
                val record=source.substring(match.range.first,end)
                val blocks=section.findAll(record).toList()
                val sections=blocks.mapIndexed { i,entry ->
                    val until=blocks.getOrNull(i+1)?.range?.first ?: record.length
                    entry.groupValues[1] to record.substring(entry.range.last+1,until).trim()
                }.toMap()
                fun part(fragment:String):String =
                    sections.entries.firstOrNull {
                        it.key.contains(fragment,ignoreCase=true) }?.value.orEmpty()
                id to ConceptDefinition(id,name,part("Core identity"),
                    part("Phase 1"),part("Symbiosis"),part("Phase 2"),
                    part("Phase 3"),part("Interpretation boundary"),
                    record)
            }.toMap()
        }
    }
}
