#include <cstdint>
#include <cstddef>
#include <cstring>
#include <array>

using CLUInt = uint32_t;
using CLULong = uint64_t;
using CLInt = int32_t;
struct ImageFormat { CLUInt order; CLUInt type; };

template <typename T>
CLInt put(const T& data, size_t capacity, void* target, size_t* actual) {
    if (actual) *actual = sizeof(data);
    if (target && capacity < sizeof(data)) return -30;
    if (target) std::memcpy(target, &data, sizeof(data));
    return 0;
}
CLInt str(const char* data, size_t capacity, void* target, size_t* actual) {
    const size_t size = std::strlen(data) + 1;
    if (actual) *actual = size;
    if (target && capacity < size) return -30;
    if (target) std::memcpy(target, data, size);
    return 0;
}
extern "C" {
CLInt clGetPlatformIDs(CLUInt capacity, void** platforms, CLUInt* count) {
#ifdef OVERSIZE_PLATFORM_COUNT
    (void)capacity; (void)platforms; if (count) *count = 65; return 0;
#elif defined(NO_PLATFORM)
    (void)capacity; (void)platforms; if (count) *count = 0; return -1001;
#else
    if (count) *count = 1;
    if (platforms && capacity > 0) platforms[0] = reinterpret_cast<void*>(0x1234);
    return 0;
#endif
}
CLInt clGetPlatformInfo(void*, CLUInt key, size_t cap, void* target, size_t* len) {
    switch(key) {
        case 0x0900: return str("FULL_PROFILE", cap, target, len);
        case 0x0901: return str("OpenCL 3.0 Mock Device", cap, target, len);
        case 0x0902: return str("Mock Platform", cap, target, len);
        case 0x0903: return str("Test Vendor", cap, target, len);
        case 0x0904: return str("cl_khr_icd", cap, target, len);
        case 0x0906: { CLUInt version = (3u << 22u); return put(version, cap, target, len); }
        case 0x0905: { CLULong resolution = 100; return put(resolution, cap, target, len); }
        default: return -30;
    }
}
CLInt clGetDeviceIDs(void*, CLULong deviceType, CLUInt capacity, void** devices, CLUInt* count) {
    if (deviceType != 0xFFFFFFFFull) return -31;
#ifdef OVERSIZE_DEVICE_COUNT
    (void)capacity; (void)devices; if (count) *count = 65; return 0;
#endif
    if (count) *count = 1;
    if (devices && capacity > 0) devices[0] = reinterpret_cast<void*>(0x5678);
    return 0;
}
#ifndef NO_DEVICEINFO
CLInt clGetDeviceInfo(void*, CLUInt key, size_t cap, void* target, size_t* len) {
#ifdef OVERSIZE_INFO
    if (key == 0x102B) { (void)cap; (void)target; if (len) *len = 65537u; return 0; }
#endif
    switch (key) {
        case 0x1000: { CLULong type = 4; return put(type, cap, target, len); }
        case 0x1002: { CLUInt units = 6; return put(units, cap, target, len); }
        case 0x1003: { CLUInt dimensions = 3; return put(dimensions, cap, target, len); }
        case 0x1004: { size_t size = 256; return put(size, cap, target, len); }
        case 0x1005: { const std::array<size_t,3> sizes = {256, 256, 64}; return put(sizes, cap, target, len); }
        case 0x1010: { CLULong size = 256ull*1024*1024; return put(size, cap, target, len); }
        case 0x1016: { CLUInt supported = 1; return put(supported, cap, target, len); }
        case 0x101F: { CLULong size = 1024ull*1024*1024; return put(size, cap, target, len); }
        case 0x1023: { CLULong size = 32ull*1024; return put(size, cap, target, len); }
        case 0x1027: case 0x1028: { CLUInt result = 1; return put(result, cap, target, len); }
        case 0x102B: return str("Mock OpenCL GPU", cap, target, len);
        case 0x102C: return str("Test Vendor", cap, target, len);
        case 0x102D: return str("Mock Driver 0.1 – Ünïcøde 😀", cap, target, len);
        case 0x102E: return str("FULL_PROFILE", cap, target, len);
        case 0x102F: return str("OpenCL 3.0 Mock", cap, target, len);
        case 0x1030: return str("cl_khr_fp16 cl_khr_int64_base_atomics", cap, target, len);
        case 0x103D: return str("OpenCL C 1.2", cap, target, len);
        default: return -30;
    }
}
#endif
void* clCreateContext(const intptr_t* props, CLUInt count, void* const* devices, void (*)(const char*, const void*, size_t, void*), void*, CLInt* error) {
    const bool valid = props && props[0] == 0x1084 && count == 1 && devices && devices[0];
    if (error) *error = valid ? 0 : -34;
    return valid ? reinterpret_cast<void*>(0x4321) : nullptr;
}
CLInt clReleaseContext(void* context) { return context == reinterpret_cast<void*>(0x4321) ? 0 : -34; }
CLInt clGetSupportedImageFormats(void*, CLULong flags, CLUInt type, CLUInt capacity, ImageFormat* data, CLUInt* count) {
    if (flags == 4 && type == 0x10F1) {
        if (count) *count = 2;
        if (data && capacity >= 2) { data[0] = {0x10B5,0x10D2}; data[1] = {0x10B6,0x10D2}; }
        return 0;
    }
    if (count) *count = 0;
    return 0;
}
}
