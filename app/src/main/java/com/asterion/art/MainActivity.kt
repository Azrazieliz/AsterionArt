package com.asterion.art

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.asterion.art.data.ArtRepository
import com.asterion.art.brain.EmbeddedInferenceInstallWorker
import com.asterion.art.brain.BrainCatalog
import com.asterion.art.brain.BrainModelStore
import com.asterion.art.brain.BrainDownloadWorker
import com.asterion.art.vision.VisionModelStore
import com.asterion.art.vision.VisionDownloadWorker
import com.asterion.art.ui.AsterionArtApp

class MainActivity : ComponentActivity() {
    private lateinit var repository: ArtRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ArtRepository(applicationContext)
        // Release builds may bundle all reasoning/vision weights. When they do
        // not, provision the same pinned GGUF assets once using WorkManager.
        // Installed weights are never copied, replaced or downloaded again.
        if (!EmbeddedInferenceInstallWorker.enqueueIfBundled(applicationContext)) {
            if (BrainModelStore(applicationContext).selected()==null)
                BrainDownloadWorker.enqueue(applicationContext,BrainCatalog.models.first().id)
            if (VisionModelStore(applicationContext).installed()==null)
                VisionDownloadWorker.enqueue(applicationContext)
        }
        setContent { AsterionArtApp(repository) }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) repository.reload()
    }
}
