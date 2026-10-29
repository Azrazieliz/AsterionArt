package com.asterion.art.methodology

import android.content.Context
import com.asterion.art.model.CreationMode

/**
 * Full original owner-authored methodology text, preserved as an APK asset.
 * The source reference stored in DetailedMethodologies is a locator, not
 * license to substitute the assistant's short rendererDirective for the
 * actual specification. Older Character Creation documents have separate
 * mutually dependent authorities and use their dedicated resolver.
 */
object MethodologyAuthority {
    fun originalText(context:Context,mode:CreationMode):String {
        require(mode!=CreationMode.CHARACTER) {
            "Character Creation requires its dedicated multi-document authority"
        }
        val contract=DetailedMethodologies.all.getValue(mode)
        val path="methodology/"+contract.referenceSource
        val text=context.assets.open(path).bufferedReader(Charsets.UTF_8)
            .use{it.readText()}
        require(text.length>=1_000 && text.contains("##")) {
            "Original owner methodology is missing or incomplete: "+path
        }
        return text
    }
}
