#pragma once

#include <cstdint>
#include <string>
#include <vector>

struct sd_dgl_ctx_t;

namespace asterion {
struct RenderRequest {
    std::string diffusion;
    std::string text_encoder;
    std::string vae;
    std::string direction;
    std::string backend = "cpu";
    int width;
    int height;
    int steps;
    int64_t seed;
    float guidance;
};

struct ReferenceAttention {
    std::vector<float> features;
    std::vector<float> patch_weights;
};

class AnimaRuntime {
public:
    explicit AnimaRuntime(int threads);
    ~AnimaRuntime();
    AnimaRuntime(const AnimaRuntime&) = delete;
    AnimaRuntime& operator=(const AnimaRuntime&) = delete;

    std::vector<uint8_t> render(const RenderRequest& request, const std::vector<ReferenceAttention>& references);
    void cancel();

private:
    sd_dgl_ctx_t* context_;
};
}
