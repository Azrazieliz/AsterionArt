#include "anima_runtime.hpp"
#include "stable-diffusion.h"

#include <fstream>
#include <iostream>
#include <stdexcept>

namespace {
std::vector<float> read_tensor(const char* path) {
    std::ifstream stream(path, std::ios::binary | std::ios::ate);
    const auto bytes = stream.tellg();
    if (bytes <= 0 || bytes % sizeof(float) != 0) throw std::runtime_error("Invalid float tensor file");
    std::vector<float> values(size_t(bytes) / sizeof(float));
    stream.seekg(0);
    if (!stream.read(reinterpret_cast<char*>(values.data()), bytes)) throw std::runtime_error("Incomplete float tensor file");
    return values;
}
}

int main(int argc, char** argv) {
    if (argc < 11 || (argc - 11) % 2 != 0) {
        std::cerr << "anima_case DIFFUSION TEXT_ENCODER VAE DIRECTION WIDTH HEIGHT STEPS SEED THREADS OUT.ppm"
                  << " [FEATURES.f32 REGION.f32 ...]\n";
        return 2;
    }
    try {
        sd_set_log_callback([](enum sd_log_level_t level, const char* message, void*) {
            if (level >= SD_LOG_WARN) std::cerr << message << '\n';
        }, nullptr);
        asterion::RenderRequest request{
            argv[1], argv[2], argv[3], argv[4], "cpu", std::stoi(argv[5]),
            std::stoi(argv[6]), std::stoi(argv[7]), std::stoll(argv[8]), 4.f,
        };
        asterion::AnimaRuntime engine(std::stoi(argv[9]));
        std::vector<asterion::ReferenceAttention> references;
        for (int i = 11; i < argc; i += 2) {
            references.push_back({read_tensor(argv[i]), read_tensor(argv[i + 1])});
        }
        asterion::ConditioningUsage usage{};
        auto pixels = engine.render(request, references, usage);
        std::ofstream output(argv[10], std::ios::binary);
        output << "P6\n" << request.width << ' ' << request.height << "\n255\n";
        output.write(reinterpret_cast<const char*>(pixels.data()), pixels.size());
        if (!output) throw std::runtime_error("Could not write the completed image");
        std::cout << "{\"text_width\":" << usage.text_width << ",\"text_positions\":" << usage.text_positions
                  << ",\"adapter_queries\":" << usage.adapter_queries << ",\"conditioning_width\":" << usage.conditioning_width
                  << ",\"conditioning_positions\":" << usage.conditioning_positions << ",\"spatial_patches\":" << usage.spatial_patches
                  << ",\"reference_positions\":[";
        for (size_t i = 0; i < usage.reference_positions.size(); ++i) {
            if (i != 0) std::cout << ',';
            std::cout << usage.reference_positions[i];
        }
        std::cout << "]}\n";
    } catch (const std::exception& failure) {
        std::cerr << failure.what() << '\n';
        return 1;
    }
}
