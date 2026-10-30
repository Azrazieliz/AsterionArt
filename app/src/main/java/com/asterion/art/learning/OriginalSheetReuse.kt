package com.asterion.art.learning

import android.content.Context
import android.net.Uri
import com.asterion.art.data.ArtRepository
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetRole
import com.asterion.art.model.TaskKind
import com.asterion.art.work.SheetMasterGenerationWorker

/**
 * Every completed Original is a reusable image authority, not a flattened
 * prompt. Conversion creates a NEW sheet generation task; it never changes
 * the originating Original or pretends a cropped catalogue is a full view.
 *
 * The original design pixels are the sole required input. More reference
 * images can be attached through the ordinary Sheet Studio afterwards.
 */
object OriginalSheetReuse {
    fun suggestedRole(design: SavedDesign): SheetRole = when(design.creationMode) {
        CreationMode.WEAPON -> SheetRole.ELEMENT
        CreationMode.STYLE_CORRECTION -> SheetRole.ART_STYLE
        else -> SheetRole.CHARACTER
    }

    /** Returns the design's own completed pixels, never its parents' pixels. */
    fun sourcePixels(design: SavedDesign): String {
        require(design.mode != RenderMode.ANIMATION) {
            "A video is not a still-image reference; select an actual saved frame."
        }
        val count=design.creationMode?.let {
            com.asterion.art.methodology.DetailedMethodologies.all[it]
                ?.catalogue?.designsPerImage
        } ?: 1
        // A catalogue sheet is NOT one candidate. Require the independent
        // preview that was rendered/extracted for this precise design.
        val source=if(count>1 && design.status=="CANDIDATE")
            design.previewUri else design.previewUri ?: design.sheetUri
        require(!source.isNullOrBlank()) {
            "This design has no individually saved image. Render it before creating a reference sheet."
        }
        return source
    }

    /**
     * Persist a new, role-specific sheet, then queue genuine reconstruction.
     * Existing sheets with identical source, role and Original are reused.
     * No automatic owner-grade or source modification occurs here.
     */
    fun create(
        context: Context,
        repository: ArtRepository,
        design: SavedDesign,
        role: SheetRole
    ): SheetRecord {
        val source=sourcePixels(design)
        val uri=Uri.parse(source)
        require(uri.scheme=="content" || uri.scheme=="file") {
            "The saved design must have a readable local image URI."
        }
        requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
            require(stream.read()>=0) { "The saved design image is empty." }
        }
        repository.reload()
        val existing=repository.sheets.firstOrNull {
            it.role==role && it.sourceUris==listOf(source) &&
                it.originatingOriginal==design.creationMode
        }
        if(existing!=null) return existing
        val sheet=repository.addSheet(
            role=role,
            sourceUris=listOf(source),
            referenceSlots=listOf("PRIMARY"),
            originatingOriginal=design.creationMode
        )
        val task=repository.recordTask(sheet.id,TaskKind.SHEET_RENDER)
        SheetMasterGenerationWorker.enqueue(context,sheet.id,task.id)
        return sheet
    }
}
