package com.asterion.compiler.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asterion.compiler.app.AsterionApplication
import com.asterion.compiler.art.AsterionArtApp

/**
 * Asterion Art is the launch screen. The previous Compiler UI remains accessible
 * as an advanced view, and the original main branch remains unchanged.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var legacyCompiler by remember { mutableStateOf(false) }
            BackHandler(enabled = legacyCompiler) { legacyCompiler = false }
            if (legacyCompiler) {
                val container = (application as AsterionApplication).container
                val compilerViewModel: CompilerViewModel = viewModel(factory = CompilerViewModel.factory(container))
                AsterionCompilerApp(viewModel = compilerViewModel)
            } else {
                AsterionArtApp(onOpenLegacyCompiler = { legacyCompiler = true })
            }
        }
    }
}
