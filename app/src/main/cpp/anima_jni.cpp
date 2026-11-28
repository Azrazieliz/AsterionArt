#include "anima_runtime.hpp"

#include <jni.h>
#include <limits>
#include <stdexcept>

namespace {
std::string read_string(JNIEnv* env, jbyteArray value) {
    if (!value) throw std::invalid_argument("Missing native Anima argument");
    std::string result(env->GetArrayLength(value), '\0');
    env->GetByteArrayRegion(value, 0, result.size(), reinterpret_cast<jbyte*>(result.data()));
    if (env->ExceptionCheck()) throw std::runtime_error("Could not read a native Anima argument");
    return result;
}

std::vector<float> read_floats(JNIEnv* env, jobjectArray array, jsize index) {
    auto row = static_cast<jfloatArray>(env->GetObjectArrayElement(array, index));
    if (!row) throw std::invalid_argument("Missing native Anima tensor");
    std::vector<float> result(env->GetArrayLength(row));
    env->GetFloatArrayRegion(row, 0, result.size(), result.data());
    env->DeleteLocalRef(row);
    if (env->ExceptionCheck()) throw std::runtime_error("Could not read a native Anima tensor");
    return result;
}

void exception(JNIEnv* env, const std::exception& problem) {
    if (!env->ExceptionCheck()) env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), problem.what());
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_asterion_art_runtime_NativeAnima_open(JNIEnv* env, jobject, jint threads) {
    try {
        return reinterpret_cast<jlong>(new asterion::AnimaRuntime(threads));
    } catch (const std::exception& problem) {
        exception(env, problem);
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_asterion_art_runtime_NativeAnima_dispose(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<asterion::AnimaRuntime*>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_asterion_art_runtime_NativeAnima_cancelRun(JNIEnv*, jobject, jlong handle) {
    reinterpret_cast<asterion::AnimaRuntime*>(handle)->cancel();
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_asterion_art_runtime_NativeAnima_execute(
    JNIEnv* env, jobject, jlong handle, jbyteArray diffusion, jbyteArray encoder, jbyteArray vae, jbyteArray direction,
    jbyteArray backend, jint width, jint height, jint steps, jlong seed, jfloat guidance,
    jobjectArray features, jobjectArray regions) {
    try {
        if (!features || !regions || env->GetArrayLength(features) != env->GetArrayLength(regions)) {
            throw std::invalid_argument("Anima features and regions are unpaired");
        }
        std::vector<asterion::ReferenceAttention> references;
        for (jsize i = 0; i < env->GetArrayLength(features); ++i) {
            references.push_back({read_floats(env, features, i), read_floats(env, regions, i)});
        }
        asterion::RenderRequest request{
            read_string(env, diffusion), read_string(env, encoder), read_string(env, vae), read_string(env, direction),
            read_string(env, backend), width, height, steps, seed, guidance,
        };
        auto result = reinterpret_cast<asterion::AnimaRuntime*>(handle)->render(request, references);
        if (result.size() > size_t(std::numeric_limits<jsize>::max())) throw std::runtime_error("Anima image exceeds JNI capacity");
        auto pixels = env->NewByteArray(result.size());
        if (pixels) env->SetByteArrayRegion(pixels, 0, result.size(), reinterpret_cast<const jbyte*>(result.data()));
        return pixels;
    } catch (const std::exception& problem) {
        exception(env, problem);
        return nullptr;
    }
}
