import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from collections import defaultdict

root = Path(__file__).resolve().parents[1]
xml_path = root / "app/src/main/assets/registry/cl.xml"
xml_data = xml_path.read_bytes()
registry = ET.fromstring(xml_data)
groups = {e.get("name"): e for e in registry.findall("enums")}

def tokens(group):
    return {e.get("name"): int(e.get("value"), 0) for e in groups[group] if e.tag == "enum" and e.get("value") and e.get("value").startswith(("0x", "0X"))}

all_tokens = {}
for group in registry.findall("enums"):
    for entry in group:
        value = entry.get("value", "")
        name = entry.get("name")
        if entry.tag == "enum" and name and re.fullmatch(r"0x[0-9a-fA-F]+", value):
            all_tokens[name] = int(value, 0)

platform_source = tokens("cl_platform_info")
device_source = tokens("cl_device_info")

def core_version_code(name):
    match = re.fullmatch(r"CL_VERSION_(\d+)_(\d+)", name or "")
    return int(match.group(1)) * 100 + int(match.group(2)) if match else 0

core_requirements = {}
for feature in registry.findall("feature"):
    if feature.get("api") not in (None, "opencl"):
        continue
    version = core_version_code(feature.get("name"))
    if not version:
        continue
    for req in feature.findall("require"):
        for entry in req.findall("enum"):
            name = entry.get("name")
            if name in all_tokens:
                value = all_tokens[name]
                prior = core_requirements.get(value)
                core_requirements[value] = version if prior is None else min(prior, version)

extension_requirements = defaultdict(set)
for extension in registry.findall("./extensions/extension"):
    supported = extension.get("supported", "")
    if "opencl" not in supported.split("|"):
        continue
    extension_name = extension.get("name", "")
    if not extension_name:
        continue
    for req in extension.findall("require"):
        if req.get("api") not in (None, "opencl"):
            continue
        for entry in req.findall("enum"):
            name = entry.get("name")
            if name in all_tokens:
                extension_requirements[all_tokens[name]].add(extension_name)
platform_types = {
    "String": ["CL_PLATFORM_NAME", "CL_PLATFORM_VENDOR", "CL_PLATFORM_VERSION", "CL_PLATFORM_PROFILE", "CL_PLATFORM_EXTENSIONS", "CL_PLATFORM_ICD_SUFFIX_KHR"],
    "U32": ["CL_PLATFORM_NUMERIC_VERSION"],
    "U64": ["CL_PLATFORM_HOST_TIMER_RESOLUTION", "CL_PLATFORM_COMMAND_BUFFER_CAPABILITIES_KHR", "CL_PLATFORM_SVM_TYPE_CAPABILITIES_KHR"],
    "Bool": ["CL_PLATFORM_UNLOADABLE_KHR"],
    "NameVersions": ["CL_PLATFORM_EXTENSIONS_WITH_VERSION"]
}
device_types = {
    "String": ["CL_DEVICE_NAME", "CL_DEVICE_VENDOR", "CL_DEVICE_VERSION", "CL_DRIVER_VERSION", "CL_DEVICE_PROFILE", "CL_DEVICE_EXTENSIONS", "CL_DEVICE_OPENCL_C_VERSION", "CL_DEVICE_SPIRV_EXTENSIONS_KHR", "CL_DEVICE_BUILT_IN_KERNELS", "CL_DEVICE_IL_VERSION", "CL_DEVICE_LATEST_CONFORMANCE_VERSION_PASSED"],
    "U32": ["CL_DEVICE_VENDOR_ID", "CL_DEVICE_MAX_COMPUTE_UNITS", "CL_DEVICE_COMMAND_BUFFER_NUM_SYNC_DEVICES_KHR", "CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS", "CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR", "CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT", "CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT", "CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG", "CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT", "CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE", "CL_DEVICE_MAX_CLOCK_FREQUENCY", "CL_DEVICE_ADDRESS_BITS", "CL_DEVICE_MAX_READ_IMAGE_ARGS", "CL_DEVICE_MAX_WRITE_IMAGE_ARGS", "CL_DEVICE_MAX_SAMPLERS", "CL_DEVICE_MEM_BASE_ADDR_ALIGN", "CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE", "CL_DEVICE_GLOBAL_MEM_CACHE_TYPE", "CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE", "CL_DEVICE_MAX_CONSTANT_ARGS", "CL_DEVICE_LOCAL_MEM_TYPE", "CL_DEVICE_PREFERRED_VECTOR_WIDTH_HALF", "CL_DEVICE_NATIVE_VECTOR_WIDTH_CHAR", "CL_DEVICE_NATIVE_VECTOR_WIDTH_SHORT", "CL_DEVICE_NATIVE_VECTOR_WIDTH_INT", "CL_DEVICE_NATIVE_VECTOR_WIDTH_LONG", "CL_DEVICE_NATIVE_VECTOR_WIDTH_FLOAT", "CL_DEVICE_NATIVE_VECTOR_WIDTH_DOUBLE", "CL_DEVICE_NATIVE_VECTOR_WIDTH_HALF", "CL_DEVICE_PARTITION_MAX_SUB_DEVICES", "CL_DEVICE_REFERENCE_COUNT", "CL_DEVICE_IMAGE_PITCH_ALIGNMENT", "CL_DEVICE_IMAGE_BASE_ADDRESS_ALIGNMENT", "CL_DEVICE_MAX_READ_WRITE_IMAGE_ARGS", "CL_DEVICE_MAX_ON_DEVICE_QUEUES", "CL_DEVICE_MAX_ON_DEVICE_EVENTS", "CL_DEVICE_MAX_PIPE_ARGS", "CL_DEVICE_PIPE_MAX_ACTIVE_RESERVATIONS", "CL_DEVICE_PIPE_MAX_PACKET_SIZE", "CL_DEVICE_PREFERRED_PLATFORM_ATOMIC_ALIGNMENT", "CL_DEVICE_PREFERRED_GLOBAL_ATOMIC_ALIGNMENT", "CL_DEVICE_PREFERRED_LOCAL_ATOMIC_ALIGNMENT", "CL_DEVICE_MAX_NUM_SUB_GROUPS", "CL_DEVICE_NUMERIC_VERSION", "CL_DEVICE_OPENCL_C_NUMERIC_VERSION_KHR", "CL_DEVICE_NODE_MASK"],
    "Bool": ["CL_DEVICE_IMAGE_SUPPORT", "CL_DEVICE_LUID_VALID", "CL_DEVICE_ERROR_CORRECTION_SUPPORT", "CL_DEVICE_ENDIAN_LITTLE", "CL_DEVICE_AVAILABLE", "CL_DEVICE_COMPILER_AVAILABLE", "CL_DEVICE_HOST_UNIFIED_MEMORY", "CL_DEVICE_LINKER_AVAILABLE", "CL_DEVICE_PREFERRED_INTEROP_USER_SYNC", "CL_DEVICE_SUB_GROUP_INDEPENDENT_FORWARD_PROGRESS", "CL_DEVICE_NON_UNIFORM_WORK_GROUP_SUPPORT", "CL_DEVICE_WORK_GROUP_COLLECTIVE_FUNCTIONS_SUPPORT", "CL_DEVICE_GENERIC_ADDRESS_SPACE_SUPPORT", "CL_DEVICE_PIPE_SUPPORT"],
    "U64": ["CL_DEVICE_TYPE", "CL_DEVICE_MAX_MEM_ALLOC_SIZE", "CL_DEVICE_KERNEL_CLOCK_CAPABILITIES_KHR", "CL_DEVICE_SVM_TYPE_CAPABILITIES_KHR", "CL_DEVICE_COMMAND_BUFFER_CAPABILITIES_KHR", "CL_DEVICE_COMMAND_BUFFER_SUPPORTED_QUEUE_PROPERTIES_KHR", "CL_DEVICE_COMMAND_BUFFER_REQUIRED_QUEUE_PROPERTIES_KHR", "CL_DEVICE_MUTABLE_DISPATCH_CAPABILITIES_KHR", "CL_DEVICE_SINGLE_FP_CONFIG", "CL_DEVICE_GLOBAL_MEM_CACHE_SIZE", "CL_DEVICE_GLOBAL_MEM_SIZE", "CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE", "CL_DEVICE_LOCAL_MEM_SIZE", "CL_DEVICE_EXECUTION_CAPABILITIES", "CL_DEVICE_QUEUE_ON_HOST_PROPERTIES", "CL_DEVICE_DOUBLE_FP_CONFIG", "CL_DEVICE_HALF_FP_CONFIG", "CL_DEVICE_PARTITION_AFFINITY_DOMAIN", "CL_DEVICE_QUEUE_ON_DEVICE_PROPERTIES", "CL_DEVICE_SVM_CAPABILITIES", "CL_DEVICE_ATOMIC_MEMORY_CAPABILITIES", "CL_DEVICE_ATOMIC_FENCE_CAPABILITIES", "CL_DEVICE_DEVICE_ENQUEUE_CAPABILITIES", "CL_DEVICE_INTEGER_DOT_PRODUCT_CAPABILITIES"],
    "Size": ["CL_DEVICE_MAX_WORK_GROUP_SIZE", "CL_DEVICE_MAX_GLOBAL_VARIABLE_SIZE", "CL_DEVICE_MAX_PARAMETER_SIZE", "CL_DEVICE_PROFILING_TIMER_RESOLUTION", "CL_DEVICE_IMAGE2D_MAX_WIDTH", "CL_DEVICE_IMAGE2D_MAX_HEIGHT", "CL_DEVICE_IMAGE3D_MAX_WIDTH", "CL_DEVICE_IMAGE3D_MAX_HEIGHT", "CL_DEVICE_IMAGE3D_MAX_DEPTH", "CL_DEVICE_IMAGE_MAX_BUFFER_SIZE", "CL_DEVICE_IMAGE_MAX_ARRAY_SIZE", "CL_DEVICE_PRINTF_BUFFER_SIZE", "CL_DEVICE_QUEUE_ON_DEVICE_PREFERRED_SIZE", "CL_DEVICE_QUEUE_ON_DEVICE_MAX_SIZE", "CL_DEVICE_GLOBAL_VARIABLE_PREFERRED_TOTAL_SIZE", "CL_DEVICE_PREFERRED_WORK_GROUP_SIZE_MULTIPLE"],
    "SizeArray": ["CL_DEVICE_MAX_WORK_ITEM_SIZES"],
    "NameVersions": ["CL_DEVICE_EXTENSIONS_WITH_VERSION", "CL_DEVICE_OPENCL_C_ALL_VERSIONS", "CL_DEVICE_ILS_WITH_VERSION", "CL_DEVICE_BUILT_IN_KERNELS_WITH_VERSION", "CL_DEVICE_OPENCL_C_FEATURES"],
    "HexBytes": ["CL_DEVICE_UUID", "CL_DRIVER_UUID", "CL_DEVICE_LUID"]
}

def typed_values(types):
    result = {}
    preferred = {}
    for kind, names in types.items():
        for name in names:
            if name not in all_tokens:
                raise ValueError(f"Registry token not found: {name}")
            value = all_tokens[name]
            prior = result.get(value)
            if prior is not None and prior != kind:
                raise ValueError(f"Conflicting type for 0x{value:X}: {prior} vs {kind}")
            result[value] = kind
            preferred.setdefault(value, name)
    return result, preferred

def normative_queries(comment, types):
    type_by_value, preferred = typed_values(types)
    required = []
    for req in registry.findall(".//require"):
        if req.get("comment") == comment:
            required.extend(e.get("name") for e in req.findall("enum") if e.get("name"))
    by_value = defaultdict(list)
    for name in dict.fromkeys(required):
        if name in all_tokens:
            by_value[all_tokens[name]].append(name)
    results = []
    aliases = {}
    vendor_suffix = re.compile(r"_(KHR|EXT|INTEL|AMD|NV|ARM|IMG|QCOM|APPLE)$")
    for value in sorted(by_value):
        names = by_value[value]
        name = preferred.get(value)
        if name not in names:
            name = sorted(names, key=lambda item: (bool(vendor_suffix.search(item)), len(item), item))[0]
        results.append({"name": name, "value": value, "kind": type_by_value.get(value, "Opaque"), "coreVersion": core_requirements.get(value, 0), "extensionsRequired": sorted(extension_requirements.get(value, set()))})
        aliases[name] = [item for item in names if item != name]
    return results, aliases

p, platform_aliases = normative_queries("cl_platform_info", platform_types)
d, device_aliases = normative_queries("cl_device_info", device_types)

def emit(xs):
    return ",\n".join(f'    {{"{x["name"]}", 0x{x["value"]:X}u, ValueKind::{x["kind"]}, {x["coreVersion"]}u, "{" ".join(x["extensionsRequired"])}"}}' for x in xs)

def image_names(low, high):
    values = {}
    for group in registry.findall("enums"):
        for e in group:
            value = e.get("value", "")
            if e.tag != "enum" or not re.fullmatch(r"0x[0-9a-fA-F]+", value):
                continue
            number_value = int(value, 16)
            if low <= number_value <= high and number_value not in values:
                values[number_value] = e.get("name")
    return values

orders = image_names(0x10B0, 0x10CF)
formats = image_names(0x10D0, 0x10EF)
header = "#pragma once\n#include <cstddef>\n#include <cstdint>\nenum class ValueKind { String, U32, U64, Bool, Size, SizeArray, NameVersions, HexBytes, Opaque };\nstruct QueryDef { const char* name; uint32_t id; ValueKind kind; uint16_t coreVersion; const char* extensionsRequired; };\nstatic constexpr QueryDef kPlatformQueries[] = {\n" + emit(p) + "\n};\nstatic constexpr QueryDef kDeviceQueries[] = {\n" + emit(d) + "\n};\n"
header += "struct EnumName { uint32_t value; const char* name; };\n"
header += "static constexpr EnumName kChannelOrders[] = {" + ",".join(f'{{0x{k:X}u,"{v}"}}' for k,v in orders.items()) + "};\n"
header += "static constexpr EnumName kChannelTypes[] = {" + ",".join(f'{{0x{k:X}u,"{v}"}}' for k,v in formats.items()) + "};\n"
(root / "app/src/main/cpp/cl_catalog.hpp").write_text(header)
all_reference = {k: {e.get("name"): e.get("value") for e in group if e.tag == "enum" and e.get("name") and e.get("value")} for k, group in groups.items() if k in ["cl_platform_info", "cl_device_info", "cl_device_type", "cl_device_svm_capabilities", "cl_device_fp_config", "ErrorCodes.0", "ErrorCodes.1000"]}
extensions = [{"name": x.get("name"), "type": x.get("type", ""), "supported": x.get("supported", "")} for x in registry.findall("./extensions/extension")]
core_features = [f.get("name") for f in registry.findall("feature") if f.get("api") in (None, "opencl") and f.get("name", "").startswith("CL_VERSION_")]
manifest = {"registrySha256": hashlib.sha256(xml_data).hexdigest(), "coreFeatures": core_features, "maxCoreFeature": core_features[-1] if core_features else "", "platformQueries": p, "deviceQueries": d, "platformAliases": platform_aliases, "deviceAliases": device_aliases, "enums": all_reference, "extensions": extensions}
(root / "app/src/main/assets/registry/catalog.json").write_text(json.dumps(manifest, ensure_ascii=False, separators=(",", ":")))
print(f"Validated OpenCL XML: {len(p)} platform queries, {len(d)} device queries, {len(extensions)} registered extensions")
