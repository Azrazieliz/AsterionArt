#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <algorithm>
#include <cstdint>
#include "llama.h"
#include "common.h"
#include "chat.h"
#include "sampling.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define VLOG(...) __android_log_print(ANDROID_LOG_WARN,"AsterionVision",__VA_ARGS__)

namespace {
std::mutex vision_mutex;
struct Session {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    mtmd_context * projector = nullptr;
    common_sampler * sampler = nullptr;
    std::unique_ptr<common_batch> batch;
    common_chat_templates_ptr templates;
    int batchSize = 1024;
    int contextSize = 4096;
    ~Session() {
        if (sampler) common_sampler_free(sampler);
        if (projector) mtmd_free(projector);
        batch.reset();
        templates.reset();
        if (ctx) llama_free(ctx);
        if (model) llama_model_free(model);
    }
};
void fail(JNIEnv *env, const char *message) {
    VLOG("%s",message);
    jclass exc=env->FindClass("java/lang/IllegalStateException");
    if(exc)env->ThrowNew(exc,message);
}
std::string utf(JNIEnv *env,jstring value) {
    if(!value)return {};
    const char *p=env->GetStringUTFChars(value,nullptr);
    if(!p)return {};
    std::string out(p);
    env->ReleaseStringUTFChars(value,p);
    return out;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_asterion_art_vision_NativeVisionSession_nativeOpen(
    JNIEnv *env, jobject, jstring llm, jstring mmproj, jint ctxSize) {
    std::lock_guard<std::mutex> lock(vision_mutex);
    if(!llm||!mmproj||ctxSize<2048||ctxSize>8192) {
        fail(env,"Vision backend requires two verified GGUF files and bounded context");
        return 0;
    }
    const auto llmPath=utf(env,llm);
    const auto projPath=utf(env,mmproj);
    auto session=std::make_unique<Session>();
    llama_backend_init();
    llama_model_params p=llama_model_default_params();
    p.n_gpu_layers=0; // Portable CPU path; do not falsely claim NPU acceleration.
    session->model=llama_model_load_from_file(llmPath.c_str(),p);
    if(!session->model) {
        fail(env,"Qwen3-VL GGUF text model could not be loaded");
        return 0;
    }
    llama_context_params cp=llama_context_default_params();
    cp.n_ctx=ctxSize;
    cp.n_batch=1024;
    cp.n_ubatch=1024;
    cp.n_threads=3;
    cp.n_threads_batch=3;
    session->ctx=llama_init_from_model(session->model,cp);
    if(!session->ctx) {
        fail(env,"Insufficient memory or unsupported Qwen3-VL context");
        return 0;
    }
    session->contextSize=ctxSize;
    mtmd_context_params mp=mtmd_context_params_default();
    mp.use_gpu=false;
    mp.n_threads=3;
    mp.warmup=false;
    mp.batch_max_tokens=512;
    mp.image_max_tokens=512;
    session->projector=mtmd_init_from_file(projPath.c_str(),session->model,mp);
    if(!session->projector||!mtmd_support_vision(session->projector)) {
        fail(env,"The projector is not compatible with the installed vision GGUF");
        return 0;
    }
    if(!llama_model_chat_template(session->model,nullptr)) {
        fail(env,"Vision model has no supported chat template");
        return 0;
    }
    session->templates=common_chat_templates_init(session->model,"");
    if(!session->templates) {
        fail(env,"Vision chat template could not be initialized");
        return 0;
    }
    common_params_sampling sampling{};
    sampling.temp=0.15f;
    session->sampler=common_sampler_init(session->model,sampling);
    if(!session->sampler) {
        fail(env,"Vision sampling initialization failed");
        return 0;
    }
    session->batch=std::make_unique<common_batch>(session->ctx);
    return reinterpret_cast<jlong>(session.release());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asterion_art_vision_NativeVisionSession_nativeAnalyze(
    JNIEnv *env,jobject,jlong handle,jbyteArray rgb,jint width,jint height,
    jstring prompt,jint outputTokens) {
    std::lock_guard<std::mutex> lock(vision_mutex);
    auto *s=reinterpret_cast<Session *>(handle);
    if(!s||!rgb||!prompt||width<8||height<8||width>768||height>768||
       outputTokens<32||outputTokens>1000) {
        fail(env,"Invalid vision input dimensions or generation budget");
        return nullptr;
    }
    const jsize bytes=env->GetArrayLength(rgb);
    if(static_cast<int64_t>(width)*height*3!=bytes) {
        fail(env,"Vision pixel buffer must contain exact RGB24 dimensions");
        return nullptr;
    }
    std::vector<unsigned char> pixels(static_cast<size_t>(bytes));
    env->GetByteArrayRegion(rgb,0,bytes,reinterpret_cast<jbyte*>(pixels.data()));
    if(env->ExceptionCheck())return nullptr;

    mtmd_bitmap *bitmap=mtmd_bitmap_init(width,height,pixels.data());
    if(!bitmap) {
        fail(env,"Unable to allocate vision input bitmap");
        return nullptr;
    }
    const auto request=utf(env,prompt);
    if(request.empty()||request.size()>48*1024) {
        mtmd_bitmap_free(bitmap);
        fail(env,"Invalid or excessive vision assessment instructions");
        return nullptr;
    }
    // Use the model's actual chat template, never a fabricated user prefix.
    const std::string marker=mtmd_default_marker();
    common_chat_msg user;
    user.role="user";
    user.content="Image: "+marker+"\n"+request;
    std::vector<common_chat_msg> noHistory;
    auto formatted=common_chat_format_single(
        s->templates.get(),noHistory,user,true,false);
    auto separator=formatted.find(marker);
    if(separator==std::string::npos) {
        mtmd_bitmap_free(bitmap);
        fail(env,"Vision chat template dropped its image marker");
        return nullptr;
    }
    const std::string before=formatted.substr(0,separator);
    const std::string after=formatted.substr(separator+marker.size());
    mtmd_input_text first{before.c_str(),before.size(),false,true};
    mtmd_input_text last{after.c_str(),after.size(),false,true};
    mtmd_input_part a{&first,nullptr};
    mtmd_input_part b{nullptr,bitmap};
    mtmd_input_part c{&last,nullptr};
    const mtmd_input_part* parts[]={&a,&b,&c};
    mtmd_input_chunks *chunks=mtmd_input_chunks_init();
    if(!chunks) {
        mtmd_bitmap_free(bitmap);
        fail(env,"Unable to initialize multimodal prompt");
        return nullptr;
    }
    const int tokenize=mtmd_tokenize_from_parts(
        s->projector,chunks,parts,3,true);
    mtmd_bitmap_free(bitmap);
    if(tokenize!=0) {
        mtmd_input_chunks_free(chunks);
        fail(env,"Native multimodal preprocessing failed");
        return nullptr;
    }
    // Evaluate the actual visual embeddings in the local neural model.
    llama_memory_clear(llama_get_memory(s->ctx),false);
    common_sampler_reset(s->sampler);
    llama_pos afterPos=0;
    const int evaluate=mtmd_helper_eval_chunks(
        s->projector,s->ctx,chunks,0,0,s->batchSize,true,&afterPos);
    mtmd_input_chunks_free(chunks);
    if(evaluate!=0 || afterPos+outputTokens+8>s->contextSize) {
        fail(env,"Multimodal context overflow or image encoding failed");
        return nullptr;
    }
    const auto *vocab=llama_model_get_vocab(s->model);
    std::vector<llama_token> generated;
    generated.reserve(outputTokens);
    for(int i=0;i<outputTokens;i++) {
        const llama_token token=common_sampler_sample(s->sampler,s->ctx,-1);
        common_sampler_accept(s->sampler,token,true);
        if(llama_vocab_is_eog(vocab,token))break;
        generated.push_back(token);
        s->batch->clear();
        s->batch->add(token,afterPos++,0,true);
        if(llama_process(s->ctx,LLAMA_PROCESS_TYPE_DECODE,s->batch->get())!=0) {
            fail(env,"Native visual reasoning decode failed");
            return nullptr;
        }
    }
    const std::string answer=common_detokenize(s->ctx,generated);
    if(answer.empty()) {
        fail(env,"Visual reasoning produced no text");
        return nullptr;
    }
    return env->NewStringUTF(answer.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_asterion_art_vision_NativeVisionSession_nativeClose(
    JNIEnv *,jobject,jlong handle) {
    std::lock_guard<std::mutex> lock(vision_mutex);
    delete reinterpret_cast<Session *>(handle);
}
