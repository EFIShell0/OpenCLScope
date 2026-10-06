package com.efishell.openclscope

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class LocalReport(val name: String, val date: Long, val bytes: Long, val device: String, val status: String)

internal object ReportStore {
    private val filePattern = Regex("^[a-f0-9]{64}\\.json$")
    private fun directory(context: Context) = File(context.filesDir, "openclscope-reports")
    private fun bytesHash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    suspend fun persist(context: Context, snapshot: CLSnapshot): Boolean = withContext(Dispatchers.IO) {
        if (snapshot.status != "ok") return@withContext false
        val bytes = snapshot.raw.toByteArray(Charsets.UTF_8)
        if (bytes.isEmpty() || bytes.size > 16 * 1024 * 1024) return@withContext false
        val folder = directory(context)
        if (!folder.isDirectory && !folder.mkdirs()) return@withContext false
        val file = File(folder, bytesHash(bytes) + ".json")
        if (file.isFile) return@withContext true
        val temporary = File.createTempFile("incoming-", ".tmp", folder)
        try {
            FileOutputStream(temporary).use { out -> out.write(bytes); out.fd.sync() }
            if (!temporary.renameTo(file)) return@withContext false
            folder.listFiles { f -> f.name.matches(filePattern) && f.isFile }?.sortedByDescending { it.lastModified() }
                ?.drop(24)?.forEach { it.delete() }
            true
        } finally { temporary.delete() }
    }

    suspend fun list(context: Context): List<LocalReport> = withContext(Dispatchers.IO) {
        directory(context).listFiles { f -> f.isFile && filePattern.matches(f.name) && f.length() in 1L..(16L * 1024 * 1024) }
            ?.sortedByDescending { it.lastModified() }?.take(24)?.mapNotNull { file ->
                runCatching {
                    val root = JSONObject(file.readText(Charsets.UTF_8))
                    val p = root.optJSONArray("platforms")?.optJSONObject(0)
                    val d = p?.optJSONArray("devices")?.optJSONObject(0)
                    LocalReport(file.name, file.lastModified(), file.length(), d?.optString("name").orEmpty().ifBlank { "No detected device" }, root.optString("status", "unknown"))
                }.getOrNull()
            }.orEmpty()
    }

    suspend fun read(context: Context, name: String): CLSnapshot = withContext(Dispatchers.IO) {
        require(filePattern.matches(name)) { "Invalid local report name" }
        val file = File(directory(context), name)
        require(file.isFile && file.length() in 1L..(16L * 1024 * 1024)) { "Report is missing or exceeds safe bound" }
        CLSnapshot.parse(file.readText(Charsets.UTF_8))
    }

    suspend fun delete(context: Context, name: String): Boolean = withContext(Dispatchers.IO) {
        require(filePattern.matches(name))
        File(directory(context), name).delete()
    }
}

internal enum class ExportFormat(val mime: String, val extension: String) {
    JSON("application/json", "json"), TXT("text/plain", "txt"), HTML("text/html", "html")
}

internal object ReportExport {
    fun encode(snapshot: CLSnapshot, format: ExportFormat): String = when (format) {
        ExportFormat.JSON -> snapshot.raw
        ExportFormat.TXT -> plain(snapshot)
        ExportFormat.HTML -> html(snapshot)
    }

    fun plain(snapshot: CLSnapshot): String = buildString {
        appendLine("OpenCLScope technical report — ${snapshot.status}")
        appendLine("Duration: ${snapshot.durationMs} ms; source: local/imported as indicated by host app")
        appendLine("Collector: ${snapshot.message}")
        snapshot.platforms.forEach { p ->
            appendLine("\nPlatform ${p.index}: ${p.name} | ${p.vendor} | ${p.version}")
            p.properties.forEach { appendLine("${it.name}: ${it.value} [${it.status}; ${it.code}]") }
            appendLine("Platform extensions: ${p.extensions.joinToString(", ")}")
            p.devices.forEach { d ->
                appendLine("\nDevice ${d.index}: ${d.name} | ${d.vendor} | ${d.version} | driver ${d.driverVersion}")
                d.properties.forEach { appendLine("${it.name}: ${it.value} [${it.status}; ${it.code}]") }
                appendLine("Device extensions: ${d.extensions.joinToString(", ")}")
                d.imageFormats.forEach { q ->
                    appendLine("Image ${q.imageType} ${q.flags}: ${q.status} (${q.code})")
                    q.formats.forEach { appendLine("  ${it.order} / ${it.dataType} [${it.orderValue}, ${it.typeValue}]") }
                }
            }
        }
    }

    private fun escape(x: String): String = buildString {
        x.forEach { c -> append(when (c) { '&' -> "&amp;"; '<' -> "&lt;"; '>' -> "&gt;"; '"' -> "&quot;"; '\'' -> "&#39;"; else -> c.toString() }) }
    }

    fun html(snapshot: CLSnapshot): String = buildString {
        append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>OpenCLScope technical report</title>")
        append("<style>body{background:#000;color:#eef8ee;font:16px system-ui;margin:auto;max-width:1000px;padding:18px}h1,h2{color:#6bd65b}table{border-collapse:collapse;width:100%;table-layout:fixed;margin:12px 0 28px}td,th{border:1px solid #334435;text-align:left;padding:8px;overflow-wrap:anywhere}tr:nth-child(even){background:#121713}.muted{color:#b4c2b3}</style></head><body>")
        append("<h1>OpenCLScope technical report</h1><p class=\"muted\">${escape(snapshot.status)} · ${snapshot.durationMs} ms · ${escape(snapshot.message)}</p>")
        fun table(title: String, props: List<Capability>) {
            append("<h2>${escape(title)}</h2><table><thead><tr><th>OpenCL token</th><th>Returned value</th><th>Query state</th></tr></thead><tbody>")
            props.forEach { p -> append("<tr><td>${escape(p.name)}</td><td>${escape(p.value)}</td><td>${escape(p.status)} (${p.code})</td></tr>") }
            append("</tbody></table>")
        }
        snapshot.platforms.forEach { p ->
            table("Platform ${p.index}: ${p.name}", p.properties)
            append("<p>Platform extensions: ${escape(p.extensions.joinToString(", "))}</p>")
            p.devices.forEach { d ->
                table("Device ${d.index}: ${d.name}", d.properties)
                append("<p>Device extensions: ${escape(d.extensions.joinToString(", "))}</p>")
                d.imageFormats.forEach { q ->
                    append("<h2>${escape(q.imageType)} — ${escape(q.flags)} — ${escape(q.status)}</h2><table><tr><th>Channel order</th><th>Type</th><th>Raw enumerants</th></tr>")
                    q.formats.forEach { f -> append("<tr><td>${escape(f.order)}</td><td>${escape(f.dataType)}</td><td>${f.orderValue}, ${f.typeValue}</td></tr>") }
                    append("</table>")
                }
            }
        }
        append("<p class=\"muted\">OpenCL™ and its logo are trademarks of The Khronos Group Inc. Independent application.</p></body></html>")
    }
}
