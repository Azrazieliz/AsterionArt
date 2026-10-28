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
        minSdk = 30
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
    lint {
        // Android Lint's NonNullableMutableLiveDataDetector crashes against
        // this Kotlin analysis API; disable only that buggy rule, not lint.
        disable += "NullSafeMutableLiveData"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
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
    // Verified Maven Central artifact; embedded Wan video runtime, no API host.
    implementation("io.github.aatricks:llmedge:0.4.7.2")
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
    // Robolectric-free JVM unit tests need a real org.json implementation.
    testImplementation("org.json:json:20240303")
    testImplementation("org.mockito:mockito-core:5.17.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
