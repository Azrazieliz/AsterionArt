package com.asterion.art.engine

import android.content.Context
import android.net.Uri
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetRole
import com.asterion.compiler.compiler.DeterministicCompilationPipeline
import com.asterion.compiler.compiler.WorksheetCompilationRequest
import com.asterion.compiler.compiler.WorksheetCompilationResult
import com.asterion.compiler.parser.ParseRequest
import com.asterion.compiler.parser.PngWorksheetSource
import com.asterion.compiler.profiles.EmbeddedProfiles
import com.asterion.compiler.worksheet.WorksheetReference
import com.asterion.compiler.worksheet.WorksheetRole
import com.asterion.compiler.worksheet.WorksheetSelection

/**
 * Executes the ORIGINAL deterministic worksheet compiler when compatible
 * structured source PNGs exist. Never impersonates the owner's five approved
 * Artwork quality phases or silently reclassifies an Original as Artwork.
 *
 * Existing unstructured artwork sources continue through the independent
 * native-reference pathway; a deterministic compilation is only returned
 * when the historical parser and validator actually accept the material.
 */
object OriginalCompilerBridge {
    data class Compiled(
        val text:String,
        val negatives:String,
        val sourceIds:List<String>,
        val passNames:List<String>
    )
    fun compile(context:Context,sheets:List<SheetRecord>):Compiled? {
        val selected=sheets.mapNotNull { sheet ->
            val role=when(sheet.role) {
                SheetRole.CHARACTER -> WorksheetRole.CHARACTER
                SheetRole.THEME -> WorksheetRole.THEME
                SheetRole.ART_STYLE -> WorksheetRole.ART_STYLE
                SheetRole.ELEMENT -> null
            } ?:return@mapNotNull null
            val uri=sheet.outputUri ?: sheet.sourceUris.singleOrNull()
                ?:return null
            val record=WorksheetReference(sheet.id,role,sheet.label,uri,"image/png")
            val bytes=context.contentResolver.openInputStream(Uri.parse(uri))?.use{
                stream->stream.readBytes()
            }?:return null
            record to ParseRequest(PngWorksheetSource(bytes),record)
        }
        val characters=selected.filter{it.first.role==WorksheetRole.CHARACTER}
        val themes=selected.filter{it.first.role==WorksheetRole.THEME}
        val styles=selected.filter{it.first.role==WorksheetRole.ART_STYLE}
        if(characters.isEmpty()||themes.size!=1||styles.size!=1)return null
        val strategy=EmbeddedProfiles.strategies.firstOrNull{
            it.id=="expanded-512"
        }?:return null
        val input=WorksheetCompilationRequest(
            selection=WorksheetSelection(
                characterSheets=characters.map{it.first},
                themeSheet=themes.single().first,
                artStyleSheet=styles.single().first
            ),
            worksheetRequests=selected.map{it.second},
            strategy=strategy
        )
        return when(val output=DeterministicCompilationPipeline().compile(input)) {
            is WorksheetCompilationResult.Blocked->null
            is WorksheetCompilationResult.Generated->{
                val passes=output.compilation.passes
                if(passes.isEmpty()||passes.any{it.positivePrompt.isBlank()})return null
                Compiled(
                    text=passes.joinToString("\n\n"){p->
                        "OWNER HISTORICAL COMPILER / "+p.type.displayName+
                            "\n"+p.positivePrompt
                    },
                    negatives=passes.map{it.negativePrompt}.filter{it.isNotBlank()}
                        .distinct().joinToString(", "),
                    sourceIds=selected.map{it.first.id},
                    passNames=passes.map{it.type.name}
                )
            }
        }
    }
}
