cmake_minimum_required(VERSION 4.1.2)
project(whisper_jni LANGUAGES C CXX)

# Shorten FetchContent paths while keeping build outputs isolated per Android ABI.
if (CMAKE_HOST_WIN32)
    get_filename_component(PARENT_DIR "${CMAKE_BINARY_DIR}" DIRECTORY)
    if (CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
        set(FETCHCONTENT_ARCH_DIR "a32")
    elseif (CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
        set(FETCHCONTENT_ARCH_DIR "a64")
    elseif (CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
        set(FETCHCONTENT_ARCH_DIR "x64")
    else ()
        set(FETCHCONTENT_ARCH_DIR "${CMAKE_ANDROID_ARCH_ABI}")
    endif ()
    set(FETCHCONTENT_BASE_DIR "${PARENT_DIR}/d/${FETCHCONTENT_ARCH_DIR}"
            CACHE PATH "Short, ABI-specific FetchContent path" FORCE)
endif ()

include(FetchContent)

if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif ()

if (${CMAKE_BUILD_TYPE} STREQUAL "Debug")
    set(IS_DEBUG TRUE)
    set(IS_RELEASE FALSE)
else ()
    set(IS_DEBUG FALSE)
    set(IS_RELEASE TRUE)
endif ()

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS ${IS_DEBUG} CACHE BOOL "" FORCE)

set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_STATIC ${IS_RELEASE} CACHE BOOL "" FORCE)
set(GGML_LTO ${IS_RELEASE} CACHE BOOL "" FORCE)

if (CMAKE_ANDROID_ARCH_ABI MATCHES "arm64")
    # Vulkan is often problematic to build on Windows host due to host tool generation issues (path length and host compiler missing)
    # Disabling by default on Windows host to ensure successful build.
    if (CMAKE_HOST_WIN32)
        set(GGML_VULKAN OFF CACHE BOOL "Enable Vulkan" FORCE)
    else ()
        set(GGML_VULKAN ON CACHE BOOL "Enable Vulkan" FORCE)
    endif ()

    if (GGML_VULKAN)
        FetchContent_Declare(vk_hpp GIT_REPOSITORY https://github.com/KhronosGroup/Vulkan-Hpp
                GIT_TAG v1.3.275
        )
        FetchContent_Declare(SPIRV-Headers GIT_REPOSITORY https://github.com/KhronosGroup/SPIRV-Headers
                GIT_TAG main
                OVERRIDE_FIND_PACKAGE
        )
        FetchContent_MakeAvailable(vk_hpp SPIRV-Headers)
        include_directories(${vk_hpp_SOURCE_DIR})
        include_directories(${SPIRV-Headers_SOURCE_DIR}/include)
        set(Vulkan_INCLUDE_DIR "${CMAKE_SYSROOT}/usr/include")

        if (CMAKE_HOST_WIN32)
            set(GLSLC_NAME glslc.exe)
        else ()
            set(GLSLC_NAME glslc)
        endif ()

        set(Vulkan_GLSLC_EXECUTABLE
            "${ANDROID_NDK}/shader-tools/${ANDROID_HOST_TAG}/${GLSLC_NAME}"
            CACHE FILEPATH "" FORCE)
        set(Vulkan_GLSLANG_VALIDATOR_EXECUTABLE "${Vulkan_GLSLC_EXECUTABLE}" CACHE FILEPATH "" FORCE)
        set(Vulkan_glslangValidator_FOUND TRUE CACHE BOOL "" FORCE)
        find_library(Vulkan_LIBRARY vulkan)
    endif ()
endif ()

find_library(LOG_LIB log)

FetchContent_Declare(
        whispercpp
        GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
        GIT_TAG master # v1.8.2
)
FetchContent_MakeAvailable(whispercpp)

add_library(whisper_jni SHARED whisper_jni.cpp)
target_link_libraries(whisper_jni PRIVATE whisper ${LOG_LIB})
target_include_directories(whisper_jni PRIVATE ${whispercpp_SOURCE_DIR})

if (CMAKE_ANDROID_ARCH_ABI MATCHES "arm64")
    target_compile_options(whisper_jni PRIVATE -march=armv8.2-a+fp16+simd+dotprod)
    if (GGML_VULKAN)
        target_link_libraries(whisper_jni PRIVATE ${Vulkan_LIBRARY})
    endif ()
elseif (CMAKE_ANDROID_ARCH_ABI STREQUAL "x86")
    set(GGML_AVX2 OFF CACHE BOOL "" FORCE)
    set(GGML_BMI2 OFF CACHE BOOL "" FORCE)
endif ()

if (IS_DEBUG)
    target_compile_options(whisper_jni PRIVATE -g -O0 -fno-omit-frame-pointer)
else ()
    target_compile_options(whisper_jni PRIVATE -O3 -ffast-math -flto)
    target_compile_options(whisper_jni PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
    target_compile_options(whisper_jni PRIVATE -ffunction-sections -fdata-sections)

    target_link_options(whisper_jni PRIVATE -flto)
    target_link_options(whisper_jni PRIVATE -Wl,--gc-sections)
    target_link_options(whisper_jni PRIVATE -Wl,--exclude-libs,ALL)
endif ()
