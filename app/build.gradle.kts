plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.asterion.art"
    compileSdk = 35
    defaultConfig {
        ndk { abiFilters += "arm64-v8a" }
        applicationId = "com.asterion.art"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }
    signingConfigs {
        create("persistentRelease") {
            val store = System.getenv("ASTERION_RELEASE_KEYSTORE")
            val pass = System.getenv("ASTERION_RELEASE_PASSWORD")
            if(!store.isNullOrBlank() && !pass.isNullOrBlank()) {
                storeFile = file(store)
                storePassword = pass
                keyAlias = "asterion-art-release"
                keyPassword = pass
                storeType = "pkcs12"
            }
        }
    }
    buildTypes {
        release {
            if (!System.getenv("ASTERION_RELEASE_KEYSTORE").isNullOrBlank() &&
                !System.getenv("ASTERION_RELEASE_PASSWORD").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("persistentRelease")
            }
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    // Large model assets are generated at packaging time, not committed to Git
    // and never fetched by the installed application.
    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("generated/embeddedModelsAssets"))
        }
    }
    androidResources {
        noCompress += "chunk"
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "**/libasterion_realsr_cli.so"
        }
    }
}
dependencies {
    implementation(project(":brain"))
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

/**
 * A release lacking the complete preverified model assets is not release-ready.
 * Debug and unit-test tasks may run without the expensive binary model package.
 */
val verifyOfflineEmbeddedModels by tasks.registering {
    doLast {
        val manifest=layout.buildDirectory.file(
            "generated/embeddedModelsAssets/asterion_bundle/manifest.json"
        ).get().asFile
        check(manifest.isFile && manifest.length()>1000L) {
            "Release requires pinned GGUF weight embedding. " +
                "Run modelpack/prepare_embedded_models.py in the build job first."
        }
        // Verify the actual staged bytes, not just a JSON manifest. This
        // deliberately adds a full streaming SHA-256 pass to release assembly
        // so a stale, truncated or tampered chunk cannot pass the source gate.
        val python=System.getenv("ASTERION_PYTHON")?.takeIf { it.isNotBlank() }
            ?: "python3"
        val checkProcess=ProcessBuilder(
            python,"modelpack/prepare_embedded_models.py",
            "--output",manifest.parentFile.parentFile.absolutePath,
            "--verify-only"
        ).directory(rootProject.projectDir)
            .redirectErrorStream(true).start()
        val report=checkProcess.inputStream.bufferedReader().use { it.readText() }
        check(checkProcess.waitFor()==0) {
            "Release neural-model byte verification failed before asset merge:\n"+
                report.takeLast(4000)
        }
        // Mandatory image-generation checkpoint: refuse incomplete packages.
        val imageManifest=manifest.parentFile.parentFile.resolve(
            "asterion_image_bundle/manifest.json")
        check(imageManifest.isFile) {
            "Release requires the full preconverted Rin Anima image weights. " +
                "Stage modelpack/prepare_embedded_image.py first."
        }
        val imageCheck=ProcessBuilder(
            python,"modelpack/prepare_embedded_image.py",
            "--output",manifest.parentFile.parentFile.absolutePath,
            "--verify-only"
        ).directory(rootProject.projectDir)
            .redirectErrorStream(true).start()
        val imageReport=imageCheck.inputStream.bufferedReader().use{it.readText()}
        check(imageCheck.waitFor()==0) {
            "Release image-model byte verification failed before asset merge:\n"+
                imageReport.takeLast(4000)
        }
    }
}
tasks.matching { it.name=="mergeReleaseAssets" }.configureEach {
    dependsOn(verifyOfflineEmbeddedModels)
}
