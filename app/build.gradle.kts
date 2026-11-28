plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
}

val buildAnima = providers.gradleProperty("asterionNativeAnima").map(String::toBoolean).orElse(false)
val studioEmulator = providers.gradleProperty("asterionStudioEmulator").map(String::toBoolean).orElse(false)

android {
    namespace = "com.asterion.art"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.asterion.art"
        minSdk = 30
        targetSdk = 36
        versionCode = 13
        versionName = "0.3.0-dev"
        ndk { abiFilters += if (studioEmulator.get() && !buildAnima.get()) "x86_64" else "arm64-v8a" }
        if (buildAnima.get()) {
            externalNativeBuild.cmake {
                arguments += "-DANDROID_STL=c++_shared"
                if (providers.gradleProperty("asterionAnimaVulkan").orNull == "true") {
                    arguments += "-DSD_VULKAN=ON"
                }
            }
        }
    }

    if (buildAnima.get()) {
        ndkVersion = "27.2.12479018"
        externalNativeBuild.cmake {
            version = "3.22.1"
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    signingConfigs {
        val originalKey = providers.environmentVariable("ASTERION_RELEASE_KEYSTORE").orNull
        val originalPassword = providers.environmentVariable("ASTERION_RELEASE_PASSWORD").orNull
        if (!originalKey.isNullOrBlank() && !originalPassword.isNullOrBlank()) {
            create("ownerRelease") {
                storeFile = file(originalKey)
                storePassword = originalPassword
                keyPassword = originalPassword
                keyAlias = providers.environmentVariable("ASTERION_RELEASE_KEY_ALIAS")
                    .orElse("asterion-art-persistent-20261028").get()
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".reconstruction"
        }
        release {
            signingConfig = signingConfigs.findByName("ownerRelease")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    implementation(project(":creation"))
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.22.0")
    testImplementation("junit:junit:4.13.2")
}
