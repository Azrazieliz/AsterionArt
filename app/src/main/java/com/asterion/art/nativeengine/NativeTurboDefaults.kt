package com.asterion.art.nativeengine

import com.asterion.art.engine.DreamModel
import com.asterion.art.model.GeneratorSettings

/**
 * Auto mode must respect the selected MODEL's distillation.
 * CircleStone Anima Turbo is trained for 8-12 steps and CFG 1.
 * Running generic CFG 4 triggers four sequential NPU graph executions
 * for every step on devices forced into low-memory mode.
 */
object NativeTurboDefaults {
    fun applies(settings:GeneratorSettings,model:DreamModel):Boolean =
        settings.automatic && model.isAnima &&
            model.name.contains("turbo",ignoreCase=true)

    fun apply(settings:GeneratorSettings,model:DreamModel,
        freeArtwork:Boolean=false):GeneratorSettings {
        if(!applies(settings,model))return settings
        return settings.copy(
            steps=8,
            cfg=1f,
            scheduler="Euler",
            // The user's validated image count is binding. Model-specific
            // speed tuning must not silently delete two of three requested
            // images; the pipeline generates them sequentially.
            batch=settings.batch
        )
    }
}
