package com.asterion.art.engine

import android.content.Context
import android.net.Uri
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetLayouts
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** OCR is only transcription of supplied sheet text, not interpretation of pictures. */
object SheetTextReader {
    suspend fun read(context: Context, sheet: SheetRecord): SourceEvidence =
        withContext(Dispatchers.IO) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                val panels = linkedMapOf<String, MutableList<String>>()
                com.asterion.art.model.SheetReferenceAuthority.primary(sheet)
                    .forEach { authority ->
                    val uri=authority.uri
                    val panel=authority.panel
                    require(SheetLayouts.isValid(sheet.role, panel)) {
                        "Sheet contains unknown panel " + panel
                    }
                    val image = InputImage.fromFilePath(context, Uri.parse(uri))
                    val result = Tasks.await(recognizer.process(image))
                    val text = result.textBlocks.flatMap { it.lines }
                        .map { it.text.trim() }
                        .filter { it.length >= 3 }
                    panels.getOrPut(panel) { mutableListOf() }.addAll(text)
                }
                SourceEvidence(sheet.id, sheet.role, panels)
            } finally {
                recognizer.close()
            }
        }
}
