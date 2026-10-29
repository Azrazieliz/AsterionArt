package com.asterion.art.model

/**
 * The completed sheet is the authoritative visual input for downstream
 * artwork. Its input photo(s) are construction provenance, not replacements
 * for the completed multi-view reference.
 *
 * Raw imported sheet images have no outputUri and are read in their original
 * panel order, without discarding multiple uploaded sheet images.
 */
object SheetReferenceAuthority {
    data class Input(val uri:String,val panel:String)
    fun primary(sheet:SheetRecord):List<Input> {
        val completed=sheet.outputUri?.trim()?.takeIf{it.isNotEmpty()}
        if(completed!=null)return listOf(Input(completed,"PRIMARY"))
        return sheet.sourceUris.mapIndexed{index,uri->
            require(uri.isNotBlank()){"Empty worksheet image source"}
            Input(uri,sheet.referenceSlots.getOrNull(index)?:"PRIMARY")
        }
    }
    fun primaryUris(sheets:List<SheetRecord>):List<String> =
        sheets.flatMap(::primary).map{it.uri}.distinct()
}
