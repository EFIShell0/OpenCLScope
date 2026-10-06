#include "cl_catalog.hpp"
#include <jni.h>
#include <dlfcn.h>
#include <algorithm>
#include <array>
#include <chrono>
#include <cstdint>
#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <dirent.h>
#include <utility>
#include <iomanip>
#include <limits>
#include <sstream>
#include <string>
#include <vector>

using CLInt = int32_t;
using CLUInt = uint32_t;
using CLULong = uint64_t;
using CLPlatform = void*;
using CLDevice = void*;
using GetPlatforms = CLInt (*)(CLUInt, CLPlatform*, CLUInt*);
using GetIcdPlatforms = CLInt (*)(CLUInt, CLPlatform*, CLUInt*);
using GetPlatformInfo = CLInt (*)(CLPlatform, CLUInt, size_t, void*, size_t*);
using GetDevices = CLInt (*)(CLPlatform, CLULong, CLUInt, CLDevice*, CLUInt*);
using GetDeviceInfo = CLInt (*)(CLDevice, CLUInt, size_t, void*, size_t*);
using CLContext = void*;
struct CLImageFormat { CLUInt order; CLUInt type; };
using CreateContext = CLContext (*)(const intptr_t*, CLUInt, const CLDevice*, void (*)(const char*, const void*, size_t, void*), void*, CLInt*);
using ReleaseContext = CLInt (*)(CLContext);
using GetImageFormats = CLInt (*)(CLContext, CLULong, CLUInt, CLUInt, CLImageFormat*, CLUInt*);

namespace {
constexpr size_t kMaxInfo = 64u * 1024u;
constexpr size_t kMaxReport = 16u * 1024u * 1024u;
constexpr CLUInt kMaxObjects = 64;
constexpr CLULong kAllDevices = 0xFFFFFFFFull;

void unicodeUnit(std::ostringstream& output, uint32_t value) {
    const char* hex = "0123456789abcdef";
    output << "\\u" << hex[(value >> 12u) & 15u] << hex[(value >> 8u) & 15u]
           << hex[(value >> 4u) & 15u] << hex[value & 15u];
}

std::string escape(const std::string& input) {
    std::ostringstream result;
    result << '"';
    for (size_t i = 0; i < input.size(); ++i) {
        const unsigned char c = static_cast<unsigned char>(input[i]);
        if (c == '"') { result << "\\\""; continue; }
        if (c == '\\') { result << "\\\\"; continue; }
        if (c == '\n') { result << "\\n"; continue; }
        if (c == '\r') { result << "\\r"; continue; }
        if (c == '\t') { result << "\\t"; continue; }
        if (c < 32) { unicodeUnit(result, c); continue; }
        if (c < 127) { result << static_cast<char>(c); continue; }
        size_t tail = 0;
        uint32_t value = 0;
        uint32_t minimum = 0;
        if (c >= 0xC2 && c <= 0xDF) { tail = 1; value = c & 31u; minimum = 0x80u; }
        else if (c >= 0xE0 && c <= 0xEF) { tail = 2; value = c & 15u; minimum = 0x800u; }
        else if (c >= 0xF0 && c <= 0xF4) { tail = 3; value = c & 7u; minimum = 0x10000u; }
        else { unicodeUnit(result, 0xFFFDu); continue; }
        bool valid = i + tail < input.size();
        if (valid) for (size_t j = 1; j <= tail; ++j) {
            const auto next = static_cast<unsigned char>(input[i + j]);
            if ((next & 0xC0u) != 0x80u) { valid = false; break; }
            value = (value << 6u) | (next & 0x3Fu);
        }
        if (!valid || value < minimum || (value >= 0xD800u && value <= 0xDFFFu) || value > 0x10FFFFu) {
            unicodeUnit(result, 0xFFFDu);
            continue;
        }
        i += tail;
        if (value <= 0xFFFFu) unicodeUnit(result, value);
        else {
            value -= 0x10000u;
            unicodeUnit(result, 0xD800u | (value >> 10u));
            unicodeUnit(result, 0xDC00u | (value & 0x3FFu));
        }
    }
    result << '"';
    return result.str();
}

std::string number(uint64_t value) { return std::to_string(value); }
std::string hexadecimal(uint64_t value) {
    std::ostringstream result;
    result << "0x" << std::uppercase << std::hex << value;
    return result.str();
}
std::string versionNumber(uint32_t value) {
    return std::to_string((value >> 22u) & 1023u) + "." +
           std::to_string((value >> 12u) & 1023u) + "." + std::to_string(value & 4095u);
}
uint16_t openCLVersionCode(const std::string& value) {
    unsigned int major = 0;
    unsigned int minor = 0;
    if (std::sscanf(value.c_str(), "OpenCL %u.%u", &major, &minor) != 2 || major > 99 || minor > 99) return 0;
    return static_cast<uint16_t>(major * 100u + minor);
}
bool extensionListed(const std::string& available, const std::string& required) {
    if (required.empty()) return false;
    std::istringstream source(available);
    std::string entry;
    while (source >> entry) if (entry == required) return true;
    return false;
}
bool queryApplicable(const QueryDef& definition, uint16_t coreVersion, const std::string& runtimeExtensions) {
    if (definition.coreVersion != 0 && coreVersion >= definition.coreVersion) return true;
    std::istringstream required(definition.extensionsRequired == nullptr ? "" : definition.extensionsRequired);
    std::string name;
    while (required >> name) if (extensionListed(runtimeExtensions, name)) return true;
    return definition.coreVersion == 0 && (definition.extensionsRequired == nullptr || *definition.extensionsRequired == 0);
}

struct Loader {
    void* handle = nullptr;
    std::string library;
    bool sawLibrary = false;
    GetPlatforms platforms = nullptr;
    GetIcdPlatforms icdPlatforms = nullptr;
    GetPlatformInfo platformInfo = nullptr;
    GetDevices devices = nullptr;
    GetDeviceInfo deviceInfo = nullptr;
    CreateContext createContext = nullptr;
    ReleaseContext releaseContext = nullptr;
    GetImageFormats getImageFormats = nullptr;

    bool bind(void* candidate, const char* name) {
        auto candidatePlatforms = reinterpret_cast<GetPlatforms>(dlsym(candidate, "clGetPlatformIDs"));
        auto candidateIcdPlatforms = reinterpret_cast<GetIcdPlatforms>(dlsym(candidate, "clIcdGetPlatformIDsKHR"));
        if (candidatePlatforms == nullptr && candidateIcdPlatforms != nullptr) candidatePlatforms = reinterpret_cast<GetPlatforms>(candidateIcdPlatforms);
        auto candidatePlatformInfo = reinterpret_cast<GetPlatformInfo>(dlsym(candidate, "clGetPlatformInfo"));
        auto candidateDevices = reinterpret_cast<GetDevices>(dlsym(candidate, "clGetDeviceIDs"));
        auto candidateDeviceInfo = reinterpret_cast<GetDeviceInfo>(dlsym(candidate, "clGetDeviceInfo"));
        if (!(candidatePlatforms && candidatePlatformInfo && candidateDevices && candidateDeviceInfo)) return false;
        handle = candidate;
        library = name;
        platforms = candidatePlatforms;
        icdPlatforms = candidateIcdPlatforms;
        platformInfo = candidatePlatformInfo;
        devices = candidateDevices;
        deviceInfo = candidateDeviceInfo;
        createContext = reinterpret_cast<CreateContext>(dlsym(candidate, "clCreateContext"));
        releaseContext = reinterpret_cast<ReleaseContext>(dlsym(candidate, "clReleaseContext"));
        getImageFormats = reinterpret_cast<GetImageFormats>(dlsym(candidate, "clGetSupportedImageFormats"));
        return true;
    }

    bool tryPath(const char* path) {
        dlerror();
        void* candidate = dlopen(path, RTLD_NOW | RTLD_LOCAL);
        if (candidate == nullptr) return false;
        sawLibrary = true;
        library = path;
        if (bind(candidate, path)) return true;
        dlclose(candidate);
        return false;
    }

    bool tryIcdDirectory(const char* directory) {
        if (directory == nullptr || *directory == 0) return false;
        DIR* dir = opendir(directory);
        if (dir == nullptr) return false;
        std::vector<std::string> files;
        while (dirent* entry = readdir(dir)) {
            std::string name = entry->d_name;
            if (name.size() > 4 && name.size() <= 192 && name.ends_with(".icd") && name.find('/') == std::string::npos && name.find("..") == std::string::npos) files.push_back(std::move(name));
            if (files.size() >= 32) break;
        }
        closedir(dir);
        std::sort(files.begin(), files.end());
        for (const auto& file : files) {
            const std::string config = std::string(directory) + "/" + file;
            FILE* input = std::fopen(config.c_str(), "rb");
            if (input == nullptr) continue;
            std::array<char, 1025> buffer{};
            const size_t count = std::fread(buffer.data(), 1, 1024, input);
            const bool overflow = count == 1024 && std::fgetc(input) != EOF;
            std::fclose(input);
            if (overflow || count == 0) continue;
            std::string path(buffer.data(), count);
            const auto lineEnd = path.find_first_of("\r\n");
            if (lineEnd != std::string::npos) path.resize(lineEnd);
            while (!path.empty() && (path.back() == ' ' || path.back() == '\t')) path.pop_back();
            size_t first = 0;
            while (first < path.size() && (path[first] == ' ' || path[first] == '\t')) ++first;
            if (first > 0) path.erase(0, first);
            if (path.empty() || path.find("..") != std::string::npos || path.find_first_of("\0\r\n") != std::string::npos) continue;
#ifdef OPENCLSCOPE_HOST_TEST
            const bool absoluteAllowed = path.starts_with("/");
#else
            const bool absoluteAllowed = path.starts_with("/vendor/") || path.starts_with("/system/vendor/") || path.starts_with("/odm/") || path.starts_with("/system/lib/") || path.starts_with("/system/lib64/");
#endif
            const bool simpleName = path.find('/') == std::string::npos && path.size() <= 192 && path.starts_with("lib") && path.ends_with(".so");
            if (!absoluteAllowed && !simpleName) continue;
            if (tryPath(path.c_str())) return true;
        }
        return false;
    }

    Loader() {
#ifdef OPENCLSCOPE_HOST_TEST
        const char* test = std::getenv("OPENCLSCOPE_TEST_LIBRARY");
        if (test != nullptr && *test != 0) tryPath(test);
        if (handle == nullptr) {
            const char* fallback = std::getenv("OPENCLSCOPE_TEST_LIBRARY_FALLBACK");
            if (fallback != nullptr && *fallback != 0) tryPath(fallback);
        }
        if (handle == nullptr) tryIcdDirectory(std::getenv("OPENCLSCOPE_TEST_ICD_DIR"));
#else
        const std::array<const char*, 19> paths = {
            "libOpenCL.so",
            "libOpenCL-pixel.so",
            "libOpenCL-car.so",
            "libOpenCL_adreno.so",
            "libGLES_mali.so",
            "libmali.so",
            "libPVROCL.so",
            "/vendor/lib64/libOpenCL.so",
            "/vendor/lib/libOpenCL.so",
            "/vendor/lib64/libOpenCL-pixel.so",
            "/vendor/lib/libOpenCL-pixel.so",
            "/vendor/lib64/egl/libGLES_mali.so",
            "/vendor/lib/egl/libGLES_mali.so",
            "/system/vendor/lib64/libOpenCL.so",
            "/system/vendor/lib/libOpenCL.so",
            "/system/lib64/libOpenCL.so",
            "/system/lib/libOpenCL.so",
            "/system/vendor/lib64/libOpenCL_adreno.so",
            "/system/vendor/lib/libPVROCL.so"
        };
        for (const char* path : paths) if (tryPath(path)) break;
        if (handle == nullptr) tryIcdDirectory("/system/vendor/Khronos/OpenCL/vendors");
        if (handle == nullptr) tryIcdDirectory("/vendor/etc/OpenCL/vendors");
#endif
    }
    Loader(const Loader&) = delete;
    Loader& operator=(const Loader&) = delete;
    ~Loader() { if (handle != nullptr) dlclose(handle); }
    bool valid() const { return handle && platforms && platformInfo && devices && deviceInfo; }
};

struct RawInfo { CLInt error; std::vector<unsigned char> bytes; };
template <typename Handle, typename Info>
RawInfo query(Info function, Handle handle, CLUInt id) {
    size_t size = 0;
    CLInt error = function(handle, id, 0, nullptr, &size);
    if (error != 0) return {error, {}};
    if (size > kMaxInfo) return {-100000, {}};
    if (size == 0) return {0, {}};
    std::vector<unsigned char> bytes(size);
    error = function(handle, id, bytes.size(), bytes.data(), nullptr);
    return {error, error == 0 ? std::move(bytes) : std::vector<unsigned char>{}};
}

template <typename T>
bool unpack(const std::vector<unsigned char>& bytes, T& value) {
    if (bytes.size() < sizeof(T)) return false;
    std::memcpy(&value, bytes.data(), sizeof(T));
    return true;
}

std::string formatInfo(const RawInfo& info, ValueKind kind, CLUInt key) {
    const auto& bytes = info.bytes;
    switch (kind) {
        case ValueKind::String: {
            const auto last = std::find(bytes.begin(), bytes.end(), 0);
            return std::string(bytes.begin(), last);
        }
        case ValueKind::U32:
        case ValueKind::Bool: {
            CLUInt value = 0;
            if (!unpack(bytes, value)) return "Invalid response length";
            if (kind == ValueKind::Bool) return value == 0 ? "false" : "true";
            if (key == 0x0906u || key == 0x105Eu || key == 0x105Fu) return versionNumber(value);
            return number(value);
        }
        case ValueKind::U64: {
            CLULong value = 0;
            if (!unpack(bytes, value)) return "Invalid response length";
            if (key == 0x1000u || key == 0x101Bu || key == 0x1032u || key == 0x1033u ||
                key == 0x1029u || key == 0x102Au || key == 0x104Eu ||
                key == 0x1053u || key == 0x1063u || key == 0x1064u || key == 0x1070u || key == 0x1073u) {
                return hexadecimal(value);
            }
            return number(value);
        }
        case ValueKind::Size: {
            size_t value = 0;
            if (!unpack(bytes, value)) return "Invalid response length";
            return number(value);
        }
        case ValueKind::SizeArray: {
            if (bytes.size() % sizeof(size_t) != 0 || bytes.size() / sizeof(size_t) > 32) return "Invalid response length";
            std::string out;
            for (size_t i = 0; i < bytes.size() / sizeof(size_t); ++i) {
                size_t value = 0;
                std::memcpy(&value, bytes.data() + i * sizeof(size_t), sizeof(size_t));
                if (!out.empty()) out += " × ";
                out += number(value);
            }
            return out;
        }
        case ValueKind::NameVersions: {
            struct NameVersion { CLUInt version; char name[64]; };
            if (bytes.size() % sizeof(NameVersion) != 0) return "Invalid response length";
            std::string out;
            for (size_t i = 0; i < bytes.size() / sizeof(NameVersion); ++i) {
                NameVersion nv{};
                std::memcpy(&nv, bytes.data() + i * sizeof(NameVersion), sizeof(NameVersion));
                const auto end = std::find(std::begin(nv.name), std::end(nv.name), '\0');
                if (!out.empty()) out += ", ";
                out += std::string(nv.name, end) + " " + versionNumber(nv.version);
            }
            return out;
        }
        case ValueKind::HexBytes: {
            if ((key == 0x106Au || key == 0x106Bu) && bytes.size() != 16) return "Invalid response length";
            if (key == 0x106Du && bytes.size() != 8) return "Invalid response length";
            std::ostringstream out;
            out << std::uppercase << std::hex << std::setfill('0');
            for (unsigned char b : bytes) out << std::setw(2) << static_cast<unsigned int>(b);
            return out.str();
        }
        case ValueKind::Opaque: {
            return std::to_string(bytes.size()) + " bytes (opaque response)";
        }
    }
    return "Invalid kind";
}

std::string property(const QueryDef& def, const RawInfo& result) {
    std::string state = "available";
    if (result.error != 0) state = result.error == -30 || result.error == -59 ? "unavailable" : "error";
    std::string value;
    CLInt code = result.error;
    if (result.error == 0) {
        value = formatInfo(result, def.kind, def.id);
        if (value == "Invalid response length" || value == "Invalid kind") { state = "error"; code = -100007; value.clear(); }
    }
    return "{\"name\":" + escape(def.name) + ",\"value\":" + escape(value) +
        ",\"status\":" + escape(state) + ",\"code\":" + std::to_string(code) + "}";
}

template <typename Handle, typename Getter, size_t N>
std::string properties(Handle handle, Getter getter, const QueryDef (&definitions)[N], uint16_t coreVersion, const std::string& runtimeExtensions) {
    std::string output = "[";
    for (const auto& definition : definitions) {
        if (output.size() > 1) output += ',';
        if (queryApplicable(definition, coreVersion, runtimeExtensions)) output += property(definition, query(getter, handle, definition.id));
        else output += "{\"name\":" + escape(definition.name) + ",\"value\":\"\",\"status\":\"not_applicable\",\"code\":0}";
    }
    output += ']';
    return output;
}

std::string readString(const RawInfo& info) {
    if (info.error != 0) return {};
    return formatInfo(info, ValueKind::String, 0);
}
std::string extensions(const std::string& names) {
    std::istringstream source(names);
    std::string name;
    std::string result = "[";
    while (source >> name) {
        if (result.size() > 1) result += ',';
        result += escape(name);
    }
    return result + ']';
}
std::string deviceType(CLULong type) {
    std::string result;
    const std::array<std::pair<CLULong, const char*>, 5> bits = {{{1u, "Default"}, {2u, "CPU"}, {4u, "GPU"}, {8u, "Accelerator"}, {16u, "Custom"}}};
    for (const auto& bit : bits) {
        if (type & bit.first) { if (!result.empty()) result += " / "; result += bit.second; }
    }
    return result.empty() ? hexadecimal(type) : result;
}

const char* imageEnum(CLUInt value, const EnumName* names, size_t count) {
    for (size_t i = 0; i < count; ++i) if (names[i].value == value) return names[i].name;
    return nullptr;
}
std::string imageFormats(Loader& opencl, CLPlatform platform, CLDevice device) {
    const auto imageSupport = query(opencl.deviceInfo, device, 0x1016u);
    CLUInt supported = 0;
    if (imageSupport.error != 0 || !unpack(imageSupport.bytes, supported)) {
        return "[{\"imageType\":\"all\",\"flags\":\"all\",\"status\":\"unavailable\",\"code\":" + std::to_string(imageSupport.error == 0 ? -100007 : imageSupport.error) + ",\"formats\":[]}]";
    }
    if (supported == 0) return "[{\"imageType\":\"all\",\"flags\":\"all\",\"status\":\"not_applicable\",\"code\":0,\"formats\":[]}]";
    if (!opencl.createContext || !opencl.releaseContext || !opencl.getImageFormats) {
        return "[{\"imageType\":\"all\",\"flags\":\"all\",\"status\":\"unavailable\",\"code\":-100005,\"formats\":[]}]";
    }
    const intptr_t props[] = {0x1084, reinterpret_cast<intptr_t>(platform), 0};
    CLInt error = 0;
    CLContext context = opencl.createContext(props, 1, &device, nullptr, nullptr, &error);
    if (error != 0 || context == nullptr) {
        return "[{\"imageType\":\"all\",\"flags\":\"all\",\"status\":\"error\",\"code\":" + std::to_string(error == 0 ? -100008 : error) + ",\"formats\":[]}]";
    }
    const std::array<std::pair<CLUInt, const char*>, 6> types = {{{0x10F4, "CL_MEM_OBJECT_IMAGE1D"}, {0x10F6, "CL_MEM_OBJECT_IMAGE1D_BUFFER"}, {0x10F1, "CL_MEM_OBJECT_IMAGE2D"}, {0x10F2, "CL_MEM_OBJECT_IMAGE3D"}, {0x10F5, "CL_MEM_OBJECT_IMAGE1D_ARRAY"}, {0x10F3, "CL_MEM_OBJECT_IMAGE2D_ARRAY"}}};
    const std::array<std::pair<CLULong, const char*>, 3> access = {{{1u, "CL_MEM_READ_WRITE"}, {2u, "CL_MEM_WRITE_ONLY"}, {4u, "CL_MEM_READ_ONLY"}}};
    std::string result = "[";
    for (const auto& type : types) {
        for (const auto& flag : access) {
            CLUInt count = 0;
            error = opencl.getImageFormats(context, flag.first, type.first, 0, nullptr, &count);
            std::vector<CLImageFormat> entries;
            if (error == 0) {
                if (count > 1024) error = -100006;
                else if (count > 0) {
                    entries.resize(count);
                    error = opencl.getImageFormats(context, flag.first, type.first, count, entries.data(), nullptr);
                }
            }
            if (result.size() > 1) result += ',';
            result += "{\"imageType\":" + escape(type.second) + ",\"flags\":" + escape(flag.second) +
                      ",\"status\":" + escape(error == 0 ? "available" : (error == -30 || error == -59 ? "unavailable" : "error")) +
                      ",\"code\":" + std::to_string(error) + ",\"formats\":[";
            if (error == 0) for (const auto& entry : entries) {
                if (result.back() != '[') result += ',';
                const char* order = imageEnum(entry.order, kChannelOrders, std::size(kChannelOrders));
                const char* dataType = imageEnum(entry.type, kChannelTypes, std::size(kChannelTypes));
                result += "{\"order\":" + escape(order ? order : hexadecimal(entry.order)) + ",\"dataType\":" +
                          escape(dataType ? dataType : hexadecimal(entry.type)) + ",\"orderValue\":" + number(entry.order) +
                          ",\"typeValue\":" + number(entry.type) + "}";
            }
            result += "]}";
        }
    }
    opencl.releaseContext(context);
    return result + ']';
}

int64_t wallClockMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}

std::string failed(std::string status, const std::string& message, const std::string& loader, CLInt error, int64_t elapsed) {
    return "{\"schema\":1,\"appVersion\":\"0.9.0\",\"collectedAtEpochMs\":" + std::to_string(wallClockMs()) + ",\"status\":" + escape(status) +
           ",\"message\":" + escape(message) + ",\"loader\":" + escape(loader) +
           ",\"errorCode\":" + std::to_string(error) + ",\"durationMs\":" +
           std::to_string(elapsed) + ",\"platforms\":[]}";
}
}

std::string collectOpenCL() {
    const auto start = std::chrono::steady_clock::now();
    auto elapsed = [&]() -> int64_t { return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - start).count(); };
    Loader opencl;
    if (!opencl.handle) return failed("unavailable", opencl.sawLibrary ? "OpenCL libraries are visible, but none expose the required core entry points" : "No declared OpenCL runtime library is accessible to this Android app process", opencl.library, opencl.sawLibrary ? -2 : -1, elapsed());
    if (!opencl.valid()) return failed("unavailable", "Required OpenCL entry points are absent", opencl.library, -2, elapsed());
    CLUInt platformCount = 0;
    CLInt err = opencl.platforms(0, nullptr, &platformCount);
    if (err == -1001 || (err == 0 && platformCount == 0)) {
        return failed("unavailable", "No OpenCL platform is exposed by the current loader", opencl.library, err, elapsed());
    }
    if (err != 0) return failed("error", "clGetPlatformIDs failed", opencl.library, err, elapsed());
    if (platformCount > kMaxObjects) return failed("error", "Platform count exceeded the safety bound", opencl.library, -100002, elapsed());
    std::vector<CLPlatform> platforms(platformCount, nullptr);
    err = opencl.platforms(platformCount, platforms.data(), nullptr);
    if (err != 0) return failed("error", "OpenCL platform enumeration failed", opencl.library, err, elapsed());
    std::string payload = "[";
    CLUInt totalDevices = 0;
    for (size_t i = 0; i < platforms.size(); ++i) {
        CLPlatform p = platforms[i];
        if (p == nullptr) continue;
        std::string name = readString(query(opencl.platformInfo, p, 0x0902u));
        std::string vendor = readString(query(opencl.platformInfo, p, 0x0903u));
        std::string version = readString(query(opencl.platformInfo, p, 0x0901u));
        std::string profile = readString(query(opencl.platformInfo, p, 0x0900u));
        std::string ext = readString(query(opencl.platformInfo, p, 0x0904u));
        if (payload.size() > 1) payload += ',';
        payload += "{\"index\":" + std::to_string(i) + ",\"name\":" + escape(name) +
                   ",\"vendor\":" + escape(vendor) + ",\"version\":" + escape(version) +
                   ",\"profile\":" + escape(profile) + ",\"extensions\":" + extensions(ext) +
                   ",\"properties\":" + properties(p, opencl.platformInfo, kPlatformQueries, openCLVersionCode(version), ext) + ",\"devices\":[";
        CLUInt count = 0;
        err = opencl.devices(p, kAllDevices, 0, nullptr, &count);
        if (err == 0 && count > kMaxObjects - totalDevices) err = -100009;
        if (err == 0) {
            totalDevices += count;
            std::vector<CLDevice> devices(count, nullptr);
            err = count == 0 ? 0 : opencl.devices(p, kAllDevices, count, devices.data(), nullptr);
            if (err == 0) {
                for (size_t j = 0; j < devices.size(); ++j) {
                    CLDevice d = devices[j];
                    if (d == nullptr) continue;
                    std::string deviceName = readString(query(opencl.deviceInfo, d, 0x102Bu));
                    std::string deviceVendor = readString(query(opencl.deviceInfo, d, 0x102Cu));
                    std::string deviceVersion = readString(query(opencl.deviceInfo, d, 0x102Fu));
                    std::string driver = readString(query(opencl.deviceInfo, d, 0x102Du));
                    std::string deviceProfile = readString(query(opencl.deviceInfo, d, 0x102Eu));
                    std::string deviceExtensions = readString(query(opencl.deviceInfo, d, 0x1030u));
                    CLULong type = 0;
                    RawInfo typeData = query(opencl.deviceInfo, d, 0x1000u);
                    const bool typed = typeData.error == 0 && unpack(typeData.bytes, type);
                    if (payload.back() != '[') payload += ',';
                    payload += "{\"index\":" + std::to_string(j) + ",\"name\":" + escape(deviceName) +
                               ",\"vendor\":" + escape(deviceVendor) + ",\"version\":" + escape(deviceVersion) +
                               ",\"driverVersion\":" + escape(driver) + ",\"profile\":" + escape(deviceProfile) +
                               ",\"type\":" + escape(typed ? deviceType(type) : "Unknown") +
                               ",\"extensions\":" + extensions(deviceExtensions) +
                               ",\"properties\":" + properties(d, opencl.deviceInfo, kDeviceQueries, openCLVersionCode(deviceVersion), deviceExtensions + " " + ext) +
                               ",\"imageFormats\":" + imageFormats(opencl, p, d) + "}";
                    if (payload.size() > kMaxReport) return failed("error", "OpenCL report exceeds its safety bound", opencl.library, -100010, elapsed());
                }
            }
        }
        if (payload.size() > kMaxReport) return failed("error", "OpenCL report exceeds its safety bound", opencl.library, -100010, elapsed());
        payload += "],\"deviceEnumerationStatus\":" + escape(err == 0 ? "available" : (err == -1 ? "no_devices" : "error")) +
                   ",\"deviceEnumerationCode\":" + std::to_string(err) + "}";
    }
    payload += ']';
    return "{\"schema\":1,\"appVersion\":\"0.9.0\",\"collectedAtEpochMs\":" + std::to_string(wallClockMs()) + ",\"status\":\"ok\",\"message\":\"Live OpenCL enumeration completed\",\"loader\":" +
           escape(opencl.library) + ",\"errorCode\":0,\"durationMs\":" + std::to_string(elapsed()) +
           ",\"platforms\":" + payload + "}";
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_efishell_openclscope_OpenCLNative_collect(JNIEnv* env, jobject) {
    try {
        const std::string report = collectOpenCL();
        return env->NewStringUTF(report.c_str());
    } catch (...) {
        return env->NewStringUTF("{\"schema\":1,\"appVersion\":\"0.9.0\",\"status\":\"error\",\"message\":\"Native collector failed\",\"loader\":\"\",\"errorCode\":-100003,\"durationMs\":0,\"platforms\":[]}");
    }
}
