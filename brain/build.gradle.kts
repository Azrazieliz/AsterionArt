plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.arm.aichat"
    compileSdk = 35
    ndkVersion = "27.2.12479018"
    defaultConfig {
        minSdk = 29
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DCMAKE_BUILD_TYPE=Release", "-DLLAMA_BUILD_TESTS=OFF",
                    "-DLLAMA_BUILD_COMMON=ON",
                    "-DLLAMA_BUILD_EXAMPLES=OFF", "-DLLAMA_CURL=OFF",
                    "-DLLAMA_OPENSSL=OFF", "-DGGML_OPENMP=OFF",
                    "-DGGML_NATIVE=OFF", "-DGGML_CPU_KLEIDIAI=OFF",
                    "-DGGML_BACKEND_DL=OFF", "-DBUILD_SHARED_LIBS=ON")
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
