package com.efishell.openclscope

import org.json.JSONArray
import org.json.JSONObject

object OpenCLNative {
    init { System.loadLibrary("openclscope") }
    external fun collect(): String
}

data class Capability(val name: String, val value: String, val status: String, val code: Int)

internal fun openCLErrorLabel(code: Int): String = when (code) {
    0 -> "CL_SUCCESS (0)"
    -1 -> "CL_DEVICE_NOT_FOUND (-1)"
    -2 -> "CL_DEVICE_NOT_AVAILABLE (-2)"
    -30 -> "CL_INVALID_VALUE (-30)"
    -31 -> "CL_INVALID_DEVICE_TYPE (-31)"
    -32 -> "CL_INVALID_PLATFORM (-32)"
    -33 -> "CL_INVALID_DEVICE (-33)"
    -34 -> "CL_INVALID_CONTEXT (-34)"
    -59 -> "CL_INVALID_OPERATION (-59)"
    -1001 -> "CL_PLATFORM_NOT_FOUND_KHR (-1001)"
    else -> "OpenCL error $code"
}

data class CLImageFormat(val order: String, val dataType: String, val orderValue: Long, val typeValue: Long)
data class CLImageQuery(val imageType: String, val flags: String, val status: String, val code: Int, val formats: List<CLImageFormat>)

data class CLDevice(
    val platformIndex: Int,
    val index: Int,
    val platformName: String,
    val name: String,
    val vendor: String,
    val version: String,
    val driverVersion: String,
    val profile: String,
    val type: String,
    val extensions: List<String>,
    val properties: List<Capability>,
    val imageFormats: List<CLImageQuery>
) {
    fun value(name: String): String? = properties.firstOrNull { it.name == name && it.status == "available" }?.value
    fun available(name: String): Boolean? = value(name)?.let { it == "true" }
}

data class CLPlatform(
    val index: Int,
    val name: String,
    val vendor: String,
    val version: String,
    val profile: String,
    val extensions: List<String>,
    val properties: List<Capability>,
    val devices: List<CLDevice>,
    val deviceEnumerationStatus: String,
    val deviceEnumerationCode: Int
)

data class CLSnapshot(
    val status: String,
    val message: String,
    val loader: String,
    val errorCode: Int,
    val durationMs: Long,
    val collectedAtEpochMs: Long,
    val platforms: List<CLPlatform>,
    val raw: String
) {
    val devices: List<CLDevice> by lazy(LazyThreadSafetyMode.NONE) { platforms.flatMap { it.devices } }
    val properties: List<Capability> by lazy(LazyThreadSafetyMode.NONE) { platforms.flatMap { it.properties + it.devices.flatMap(CLDevice::properties) } }
    companion object {
        fun parse(text: String): CLSnapshot {
            val root = JSONObject(text)
            require((root.optJSONArray("platforms")?.length() ?: 0) <= 64) { "Platform count exceeds safe import bound" }
            val platforms = root.optJSONArray("platforms").objects().map { p ->
                require((p.optJSONArray("devices")?.length() ?: 0) <= 64) { "Device count exceeds safe import bound" }
                require((p.optJSONArray("properties")?.length() ?: 0) <= 512) { "Platform property count exceeds safe import bound" }
                val index = p.optInt("index")
                val title = p.optString("name")
                CLPlatform(
                    index, title, p.optString("vendor"), p.optString("version"), p.optString("profile"),
                    p.optJSONArray("extensions").strings(), p.optJSONArray("properties").capabilities(),
                    p.optJSONArray("devices").objects().map { d ->
                        require((d.optJSONArray("properties")?.length() ?: 0) <= 512) { "Device property count exceeds safe import bound" }
                        require((d.optJSONArray("imageFormats")?.length() ?: 0) <= 24) { "Image format group count exceeds safe import bound" }
                        CLDevice(index, d.optInt("index"), title, d.optString("name"), d.optString("vendor"),
                            d.optString("version"), d.optString("driverVersion"), d.optString("profile"),
                            d.optString("type"), d.optJSONArray("extensions").strings(), d.optJSONArray("properties").capabilities(),
                            d.optJSONArray("imageFormats").objects().map { image ->
                                CLImageQuery(image.optString("imageType"), image.optString("flags"), image.optString("status"), image.optInt("code"),
                                    image.optJSONArray("formats").objects().map { value ->
                                        CLImageFormat(value.optString("order"), value.optString("dataType"), value.optLong("orderValue"), value.optLong("typeValue"))
                                    })
                            })
                    },
                    p.optString("deviceEnumerationStatus", "unknown"), p.optInt("deviceEnumerationCode")
                )
            }
            return CLSnapshot(root.optString("status", "error"), root.optString("message"), root.optString("loader"),
                root.optInt("errorCode"), root.optLong("durationMs"), root.optLong("collectedAtEpochMs"), platforms, text)
        }
        fun failure(message: String): CLSnapshot = parse(
            JSONObject().put("schema", 1).put("appVersion", "0.9.0").put("status", "error")
                .put("message", message).put("loader", "").put("errorCode", -100004)
                .put("durationMs", 0).put("platforms", JSONArray()).toString()
        )
    }
}

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else
    (0 until length()).mapNotNull { optJSONObject(it) }
private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else
    (0 until length()).map { optString(it) }
private fun JSONArray?.capabilities(): List<Capability> = objects().map {
    Capability(it.optString("name"), it.optString("value"), it.optString("status", "unknown"), it.optInt("code"))
}
