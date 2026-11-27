package com.asterion.art.nativeengine

import com.asterion.art.engine.DreamModel
import com.asterion.art.model.SheetRole

/**
 * Select a native reference-conditioning engine for actual sheet pixels.
 * A generic anime checkpoint cannot silently collapse multiple sources to
 * a single img2img input. An explicitly assigned sheet model is authoritative
 * only when it can represent every supplied reference image.
 */
object SheetModelSelection {
    private val referenceKinds=setOf("qwen21","klein")

    fun select(role:SheetRole,models:List<DreamModel>,
        assigned:DreamModel?,preferred:DreamModel?,sourceCount:Int):DreamModel {
        require(sourceCount in 1..10) {
            "A native sheet requires 1–10 real source images"
        }
        require(models.isNotEmpty()) { "No native image model installed for "+role.display }
        val compatible=if(sourceCount>1)
            models.filter { it.ditKind in referenceKinds }
            else models
        require(compatible.isNotEmpty()) {
            "Selected "+role.display+" Sheet contains "+sourceCount+" sources. "+
                "Install a Qwen/Klein native reference-capable model; "+
                "one-reference img2img would discard source identities."
        }
        if(assigned!=null) {
            require(compatible.any { it.id==assigned.id }) {
                "Explicit "+role.display+" Sheet model cannot accept "+
                    sourceCount+" original reference images"
            }
            return compatible.first { it.id==assigned.id }
        }
        // When available, native visual-reference channels are a stronger
        // foundation for analytic sheets than a one-image repaint model.
        return compatible.firstOrNull { it.ditKind in referenceKinds }
            ?:compatible.firstOrNull { it.id==preferred?.id }
            ?:compatible.first()
    }
}
