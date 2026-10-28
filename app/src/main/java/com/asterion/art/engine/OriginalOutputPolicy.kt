package com.asterion.art.engine

import com.asterion.art.model.CreationMode

/**
 * An Original catalogue counts DESIGN CANDIDATES inside its prescribed image,
 * not independent copies of the same catalogue. Only Character v3 uses the
 * user-chosen 1..30 sheet population; free Artwork keeps its batch setting.
 */
object OriginalOutputPolicy {
    fun imageCount(mode:CreationMode?,individualPrompt:Boolean,
        characterPhaseOneSheets:Int?,artworkBatch:Int):Int {
        if(individualPrompt)return 1
        if(mode==CreationMode.CHARACTER) {
            require(characterPhaseOneSheets in 1..30) {
                "Character v3 requires a frozen 1–30 sheet plan"
            }
            return requireNotNull(characterPhaseOneSheets)
        }
        if(mode!=null)return 1
        return artworkBatch.coerceIn(1,8)
    }
}
