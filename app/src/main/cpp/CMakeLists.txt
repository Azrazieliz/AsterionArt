cmake_minimum_required(VERSION 3.22)
project(asterion_anima LANGUAGES C CXX)

include(FetchContent)
find_package(Git REQUIRED)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(SD_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SD_WEBP OFF CACHE BOOL "" FORCE)
set(SD_WEBM OFF CACHE BOOL "" FORCE)
set(SD_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
if(ANDROID)
    set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
    set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
endif()

FetchContent_Declare(asterion_diffusion
    GIT_REPOSITORY https://github.com/leejet/stable-diffusion.cpp.git
    GIT_TAG 58d1126febec89040ead4e136d7467a0e6fe6746
    GIT_SUBMODULES ggml
    PATCH_COMMAND ${GIT_EXECUTABLE} apply --unidiff-zero ${CMAKE_CURRENT_LIST_DIR}/anima-reference-attention.patch
)
FetchContent_MakeAvailable(asterion_diffusion)

add_library(asterion_anima_runtime STATIC anima_runtime.cpp)
target_compile_features(asterion_anima_runtime PUBLIC cxx_std_17)
target_link_libraries(asterion_anima_runtime PUBLIC stable-diffusion)

if(ANDROID)
    add_library(asterion_anima SHARED anima_jni.cpp)
    target_link_libraries(asterion_anima PRIVATE asterion_anima_runtime)
else()
    add_executable(asterion_anima_case anima_case.cpp)
    target_link_libraries(asterion_anima_case PRIVATE asterion_anima_runtime)
endif()
