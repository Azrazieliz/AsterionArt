package com.asterion.art

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.asterion.art.data.ArtRepository
import com.asterion.art.brain.EmbeddedInferenceInstallWorker
import com.asterion.art.ui.AsterionArtApp

class MainActivity : ComponentActivity() {
    private lateinit var repository: ArtRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ArtRepository(applicationContext)
        EmbeddedInferenceInstallWorker.enqueueIfBundled(applicationContext)
        setContent { AsterionArtApp(repository) }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) repository.reload()
    }
}
