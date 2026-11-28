#include "anima_runtime.hpp"
#include "stable-diffusion.h"

#include <algorithm>
#include <cmath>
#include <iomanip>
#include <limits>
#include <locale>
#include <memory>
#include <sstream>
#include <stdexcept>
#include <utility>

namespace asterion {
namespace {
constexpr int anima_features = 1024;
constexpr int patch_pixels = 16;

std::string quoted(const std::string& value) {
    std::ostringstream escaped;
    escaped << '"';
    for (unsigned char character : value) {
        switch (character) {
            case '"': escaped << "\\\""; break;
            case '\\': escaped << "\\\\"; break;
            case '\n': escaped << "\\n"; break;
            case '\r': escaped << "\\r"; break;
            case '\t': escaped << "\\t"; break;
            default:
                if (character < 0x20) {
                    throw std::invalid_argument("Unexpected control character in the Anima input");
                }
                escaped << character;
        }
    }
    escaped << '"';
    return escaped.str();
}

std::string program(const RenderRequest& request, const std::vector<std::string>& inputs,
                    const std::vector<std::string>& attention, bool learned_only, bool trained_unconditional) {
    std::ostringstream graph;
    graph.imbue(std::locale::classic());
    graph << "fn main(";
    for (size_t i = 0; i < inputs.size(); ++i) {
        if (i != 0) graph << ", ";
        graph << inputs[i];
    }
    graph << ") {\n"
          << "    model = load_dit(" << quoted(request.diffusion) << ", family = \"anima\", backend = "
          << quoted(request.backend) << ", params_backend = \"cpu\")\n";
    if (!learned_only || !trained_unconditional) {
        graph << "    text = load_text_encoder(" << quoted(request.text_encoder)
              << ", family = \"anima\", backend = \"cpu\")\n";
    }
    graph << "    vae = load_vae(" << quoted(request.vae) << ", family = \"anima\", backend = \"cpu\")\n"
          << "    base = tensor::zeros(latent_shape(vae, width = " << request.width
          << ", height = " << request.height << "))\n";
    if (!learned_only) {
        graph << "    encoded = encode_prompt(text, " << quoted(request.direction)
              << ", width = " << request.width << ", height = " << request.height << ", raw = true)\n"
              << "    authored = anima::prepare_context(model, encoded)\n";
    }
    if (!trained_unconditional) {
        graph << "    empty = encode_prompt(text, \"\", width = " << request.width << ", height = " << request.height
              << ", raw = true)\n"
              << "    negative_context = anima::prepare_context(model, empty)\n";
    }
    const char* positive = learned_only ? "design" : "authored";
    const char* negative = trained_unconditional ? "unconditional" : "negative_context";
    graph << "    positive = predictor(model, {crossattn = " << positive;
    if (!attention.empty()) {
        graph << ", extra_crossattn = [";
        for (size_t i = 0; i < attention.size(); ++i) {
            if (i != 0) graph << ", ";
            graph << attention[i];
        }
        graph << "]";
    }
    graph << "})\n"
          << "    guided = cfg(positive, predictor(model, {crossattn = " << negative << "}), scale = "
          << std::setprecision(std::numeric_limits<float>::max_digits10) << request.guidance << ")\n"
          << "    sigmas = make_sigmas(guided, latent = base, steps = " << request.steps
          << ", scheduler = \"beta\")\n"
          << "    rng = make_rng(\"cpu\", seed = " << request.seed << ")\n"
          << "    noisy = add_noise(guided, base, tensor::randn_like(base, rng = rng), sigma = sigmas[0])\n"
          << "    latent = sample(noisy, sigmas, predict = guided, method = \"euler\", rng = rng)\n"
          << "    image = vae_decode(vae, dit_to_vae_latents(vae, latent))\n"
          << "    return {image = image, condition_shape = tensor::shape(" << positive
          << "), unconditional_shape = tensor::shape(" << negative << ")";
    if (!learned_only) graph << ", text_shape = tensor::shape(encoded.crossattn), t5_shape = tensor::shape(encoded.t5_ids)";
    graph << "}\n}\n";
    return graph.str();
}

using Tensor = std::unique_ptr<sd_tensor_t, decltype(&sd_tensor_free)>;

Tensor tensor(const std::vector<float>& values, int64_t width, int64_t height) {
    const int64_t shape[] = {width, height};
    auto input = Tensor(
        sd_tensor_create(SD_TENSOR_F32, 2, shape, values.data(), values.size() * sizeof(float)),
        sd_tensor_free);
    if (!input) throw std::runtime_error(sd_get_last_error());
    return input;
}

void check_finite(const std::vector<float>& values, bool nonnegative) {
    if (!std::all_of(values.begin(), values.end(), [nonnegative](float value) {
        return std::isfinite(value) && (!nonnegative || value >= 0.f);
    })) throw std::invalid_argument("Anima conditioning contains invalid values");
}

int64_t axis_length(const sd_dgl_result_t* result, const char* name, size_t axis) {
    const auto* shape = sd_dgl_result_tensor(result, name);
    if (!shape || sd_tensor_rank(shape) != 1 || sd_tensor_type(shape) != SD_TENSOR_I64 ||
        sd_tensor_dim(shape, 0) <= static_cast<int64_t>(axis)) {
        throw std::runtime_error("Anima did not report its conditioning dimensions");
    }
    const auto* dimensions = static_cast<const int64_t*>(sd_tensor_data(shape));
    if (!dimensions || dimensions[axis] <= 0) {
        throw std::runtime_error("Anima reported an invalid conditioning dimension");
    }
    return dimensions[axis];
}
}

AnimaRuntime::AnimaRuntime(int threads) : context_(sd_dgl_ctx_create_once()) {
    if (!context_) throw std::runtime_error(sd_get_last_error());
    if (threads <= 0 || !sd_dgl_ctx_set(context_, "n_threads", std::to_string(threads).c_str()) ||
        !sd_dgl_ctx_set(context_, "enable_mmap", "true") ||
        !sd_dgl_ctx_set(context_, "embed_metadata", "off")) {
        const std::string error = sd_get_last_error();
        sd_dgl_ctx_free(context_);
        throw std::runtime_error("Could not initialize Anima: " + error);
    }
}

AnimaRuntime::~AnimaRuntime() {
    sd_dgl_ctx_free(context_);
}

void AnimaRuntime::cancel() {
    sd_dgl_ctx_cancel(context_);
}

std::vector<uint8_t> AnimaRuntime::render(const RenderRequest& request, const std::vector<ReferenceAttention>& references,
                                         ConditioningUsage& usage, const LearnedDesign& design) {
    if (request.width <= 0 || request.height <= 0 || request.width % patch_pixels != 0 ||
        request.height % patch_pixels != 0 || request.steps <= 0 || !std::isfinite(request.guidance) ||
        (request.backend != "cpu" && request.backend != "vulkan0")) {
        throw std::invalid_argument("Invalid Anima rendering geometry or runtime");
    }
    const auto patches = int64_t(request.width / patch_pixels) * (request.height / patch_pixels);
    std::vector<Tensor> tensors;
    std::vector<std::string> names;
    std::vector<std::string> attention;
    std::vector<sd_dgl_tensor_input_t> bindings;
    std::vector<int64_t> reference_positions;
    const auto bind_features = [&](const std::string& name, const std::vector<float>& features) {
        if (features.empty() || features.size() % anima_features != 0) {
            throw std::invalid_argument("Anima conditioning needs complete 1,024-wide positions");
        }
        check_finite(features, false);
        names.push_back(name);
        const auto positions = static_cast<int64_t>(features.size() / anima_features);
        tensors.push_back(tensor(features, anima_features, positions));
        return positions;
    };
    const bool learned_only = !design.positive.empty() && request.direction.empty();
    int64_t design_positions = 0;
    if (!design.positive.empty()) {
        design_positions = bind_features("design", design.positive);
        if (!learned_only) {
            names.push_back("design_region");
            tensors.push_back(tensor(std::vector<float>(patches, 1.f), 1, patches));
            attention.insert(attention.end(), {"design", "design_region"});
        }
    }
    if (!design.unconditional.empty()) bind_features("unconditional", design.unconditional);
    for (size_t i = 0; i < references.size(); ++i) {
        const auto& reference = references[i];
        if (reference.patch_weights.size() != size_t(patches)) {
            throw std::invalid_argument("Anima reference regions have the wrong number of output patches");
        }
        check_finite(reference.patch_weights, true);
        const auto features = "features_" + std::to_string(i);
        const auto region = "region_" + std::to_string(i);
        const auto positions = bind_features(features, reference.features);
        names.push_back(region);
        attention.insert(attention.end(), {features, region});
        tensors.push_back(tensor(reference.patch_weights, 1, patches));
        reference_positions.push_back(positions);
    }
    for (size_t i = 0; i < tensors.size(); ++i) {
        bindings.push_back({names[i].c_str(), tensors[i].get()});
    }

    sd_dgl_result_t* raw = nullptr;
    const auto source = program(request, names, attention, learned_only, !design.unconditional.empty());
    const auto status = sd_dgl_run(context_, source.c_str(), bindings.data(), bindings.size(), &raw);
    const auto result = std::unique_ptr<sd_dgl_result_t, decltype(&sd_dgl_result_free)>(raw, sd_dgl_result_free);
    if (status != SD_STATUS_OK) throw std::runtime_error(sd_get_last_error());
    const auto* image = sd_dgl_result_tensor(result.get(), "image");
    if (!image || sd_tensor_type(image) != SD_TENSOR_U8 || sd_tensor_rank(image) != 3 ||
        sd_tensor_dim(image, 0) != 3 || sd_tensor_dim(image, 1) != request.width ||
        sd_tensor_dim(image, 2) != request.height) {
        throw std::runtime_error("Anima returned an unexpected image");
    }
    const auto* bytes = static_cast<const uint8_t*>(sd_tensor_data(image));
    if (!bytes) throw std::runtime_error(sd_get_last_error());
    usage = {
        learned_only ? 0 : axis_length(result.get(), "text_shape", 0),
        learned_only ? 0 : axis_length(result.get(), "text_shape", 1),
        learned_only ? 0 : axis_length(result.get(), "t5_shape", 0),
        axis_length(result.get(), "condition_shape", 0), axis_length(result.get(), "condition_shape", 1),
        design_positions, axis_length(result.get(), "unconditional_shape", 1), patches, std::move(reference_positions),
    };
    return {bytes, bytes + sd_tensor_byte_size(image)};
}
}
