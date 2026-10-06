package com.efishell.openclscope

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

internal data class PublicReport(
    val id: String,
    val submittedAt: String,
    val deviceName: String,
    val vendor: String,
    val platformName: String,
    val openclVersion: String,
    val driverVersion: String,
    val model: String,
    val platformCount: Int,
    val deviceCount: Int
)

internal data class ReportCursor(val submittedAt: String, val id: String)
internal data class PublicReportPage(val reports: List<PublicReport>, val nextCursor: ReportCursor?)
internal data class ReportReceipt(val id: String, val submittedAt: String, val status: String)

internal object DatabaseClient {
    private const val maxReportBytes = 16 * 1024 * 1024
    private const val maxResponseBytes = 18 * 1024 * 1024
    private const val maxPageBytes = 512 * 1024
    private val idPattern = Regex("[a-f0-9]{64}")
    private val timestampPattern = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z")
    private val apiUrl = BuildConfig.DATABASE_API_BASE_URL
    private val webUrl = BuildConfig.DATABASE_WEB_BASE_URL
    val configured: Boolean get() = apiUrl.isNotBlank()
    val webConfigured: Boolean get() = webUrl.isNotBlank()

    fun hasValidatedInternet(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun eligible(report: CLSnapshot?, origin: String, collecting: Boolean): Boolean {
        if (report == null || collecting || origin != "Live" || report.status != "ok" || report.devices.isEmpty()) return false
        if (report.platforms.any { it.deviceEnumerationStatus !in setOf("available", "no_devices") }) return false
        if (report.platforms.any { p -> p.deviceEnumerationStatus == "no_devices" && p.devices.isNotEmpty() }) return false
        return true
    }

    fun completeEnvelope(context: Context, report: CLSnapshot): JSONObject {
        require(eligible(report, "Live", false)) { "Only fully enumerated, live reports can be submitted" }
        val original = JSONObject(report.raw)
        require(original.optInt("schema") == 1 && original.optString("appVersion") == "0.9.0") { "Unsupported collector report version" }
        val catalog = JSONObject(context.assets.open("registry/catalog.json").bufferedReader().use { it.readText() })
        val platforms = original.getJSONArray("platforms")
        require(platforms.length() == report.platforms.size && platforms.length() in 1..64)
        var deviceCount = 0
        for (i in 0 until platforms.length()) {
            val p = platforms.getJSONObject(i)
            require(p.getJSONArray("properties").length() == catalog.getJSONArray("platformQueries").length()) { "Platform query set is incomplete" }
            val devices = p.getJSONArray("devices")
            deviceCount += devices.length()
            for (j in 0 until devices.length()) {
                val d = devices.getJSONObject(j)
                require(d.getJSONArray("properties").length() == catalog.getJSONArray("deviceQueries").length()) { "Device query set is incomplete" }
                require(d.has("imageFormats")) { "Image format query state is missing" }
            }
        }
        require(deviceCount in 1..64) { "No device was enumerated" }
        return JSONObject()
            .put("schemaVersion", 1)
            .put("application", JSONObject()
                .put("name", "OpenCLScope")
                .put("packageName", BuildConfig.APPLICATION_ID)
                .put("versionName", BuildConfig.VERSION_NAME)
                .put("versionCode", BuildConfig.VERSION_CODE))
            .put("device", JSONObject()
                .put("manufacturer", Build.MANUFACTURER.take(120))
                .put("model", Build.MODEL.take(120))
                .put("androidApi", Build.VERSION.SDK_INT))
            .put("collection", JSONObject().put("status", "complete").put("source", "live"))
            .put("registrySha256", catalog.getString("registrySha256"))
            .put("technicalReport", original)
    }

    suspend fun submit(context: Context, report: CLSnapshot): ReportReceipt = withContext(Dispatchers.IO) {
        if (!hasValidatedInternet(context)) error("A validated Internet connection is required")
        val text = completeEnvelope(context, report).toString()
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= maxReportBytes) { "The complete report exceeds the 16 MiB database limit. No fields have been truncated." }
        val result = request("/v1/reports", "POST", bytes, maxPageBytes)
        ReportReceipt(
            id = result.getString("id").also { require(idPattern.matches(it)) },
            submittedAt = result.getString("submittedAt").also { require(timestampPattern.matches(it)) },
            status = result.getString("status").also { require(it == "accepted" || it == "duplicate") }
        )
    }

    suspend fun list(context: Context, cursor: ReportCursor? = null): PublicReportPage = withContext(Dispatchers.IO) {
        if (!hasValidatedInternet(context)) error("A validated Internet connection is required")
        if (cursor != null) require(idPattern.matches(cursor.id) && timestampPattern.matches(cursor.submittedAt))
        val query = if (cursor == null) "?limit=25" else
            "?limit=25&beforeSubmittedAt=${encode(cursor.submittedAt)}&beforeId=${encode(cursor.id)}"
        val body = request("/v1/reports$query", "GET", null, maxPageBytes)
        val reports = body.getJSONArray("reports")
        require(reports.length() <= 25) { "Database page exceeded its bound" }
        val entries = (0 until reports.length()).map { i ->
            val item = reports.getJSONObject(i)
            PublicReport(
                id = item.getString("id").also { require(idPattern.matches(it)) },
                submittedAt = item.getString("submitted_at").also { require(timestampPattern.matches(it)) },
                deviceName = item.optString("device_name"), vendor = item.optString("vendor"),
                platformName = item.optString("platform_name"), openclVersion = item.optString("opencl_version"),
                driverVersion = item.optString("driver_version"), model = item.optString("model"),
                platformCount = item.optInt("platform_count"), deviceCount = item.optInt("device_count")
            )
        }
        val next = body.optJSONObject("nextCursor")?.let { token ->
            val id = token.optString("id")
            val time = token.optString("submittedAt")
            if (idPattern.matches(id) && timestampPattern.matches(time)) ReportCursor(time, id) else error("Database pagination cursor is invalid")
        }
        PublicReportPage(entries, next)
    }

    suspend fun fetch(context: Context, reportId: String): JSONObject = withContext(Dispatchers.IO) {
        if (!hasValidatedInternet(context)) error("A validated Internet connection is required")
        val id = reportId.trim().lowercase(java.util.Locale.ROOT)
        require(idPattern.matches(id)) { "Report ID must contain exactly 64 hexadecimal characters" }
        val body = request("/v1/reports/$id", "GET", null, maxResponseBytes)
        require(body.getString("id") == id && body.optInt("schemaVersion") == 1) { "Database report identity/schema mismatch" }
        require(body.has("technicalReport")) { "Database technicalReport is absent" }
        body
    }

    fun permalink(receiptId: String): String? {
        if (!webConfigured || !idPattern.matches(receiptId)) return null
        return webUrl.trimEnd('/') + "/#reports/" + receiptId + "/Overview"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    private fun request(path: String, method: String, body: ByteArray?, maximum: Int): JSONObject {
        require(configured) { "Database endpoint is not configured in this build" }
        val uri = URI(apiUrl)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null &&
            (uri.port == -1 || uri.port == 443) && (uri.path.isNullOrEmpty() || uri.path == "/")) { "Invalid fixed database HTTPS endpoint" }
        require(path.startsWith("/v1/"))
        val conn = uri.resolve(path).toURL().openConnection() as HttpsURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.requestMethod = method
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "OpenCLScope/${BuildConfig.VERSION_NAME}")
        if (body != null) {
            require(method == "POST")
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setFixedLengthStreamingMode(body.size)
        }
        try {
            if (body != null) conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val reason = runCatching { conn.errorStream?.use { limitedText(it, maxPageBytes) } }.getOrNull()
                val message = runCatching { JSONObject(reason ?: "{}").optString("error") }.getOrNull().orEmpty()
                throw IOException("Database HTTP $code: ${message.ifBlank { "Request failed" }}")
            }
            val value = conn.inputStream.use { limitedText(it, maximum) }
            return JSONObject(value)
        } finally { conn.disconnect() }
    }

    private fun limitedText(input: java.io.InputStream, limit: Int): String {
        val out = ByteArrayOutputStream()
        val block = ByteArray(8192)
        while (true) {
            val n = input.read(block)
            if (n == -1) break
            if (n == 0) throw IOException("Response stream stalled")
            if (out.size() + n > limit) throw IOException("Database response exceeds the safety bound")
            out.write(block, 0, n)
        }
        val decoder = Charsets.UTF_8.newDecoder()
        decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
        decoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        return decoder.decode(java.nio.ByteBuffer.wrap(out.toByteArray())).toString()
    }
}
