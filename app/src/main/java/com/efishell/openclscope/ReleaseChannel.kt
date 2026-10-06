package com.efishell.openclscope

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.net.ssl.HttpsURLConnection

internal data class GitHubRelease(val tag: String, val published: String, val page: String, val universalApk: String?, val assetSize: Long)

internal object ReleaseChannel {
    private val repository = BuildConfig.RELEASE_GITHUB_REPOSITORY
    val configured get() = repository.isNotBlank()
    private fun slug(): String = repository.removePrefix("https://github.com/").also {
        require(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+").matches(it))
    }
    fun sourceUrl() = repository
    fun obtainiumJson() = JSONObject()
        .put("id", BuildConfig.APPLICATION_ID)
        .put("url", repository)
        .put("author", slug().substringBefore('/'))
        .put("name", "OpenCLScope")
        .put("preferredApkIndex", 0)
        .put("overrideSource", "GitHub")
        .put("additionalSettings", JSONObject()
            .put("includePrereleases", false)
            .put("fallbackToOlderReleases", false)
            .put("apkFilterRegEx", "(?i).*universal.*\\.apk$")
            .put("invertAPKFilter", false)
            .put("autoApkFilterByArch", false)
            .put("trackOnly", false)
            .put("appName", "OpenCLScope").toString()).toString()

    fun openObtainium(context: Context): Boolean {
        if (!configured) return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("obtainium://app/${Uri.encode(obtainiumJson())}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { context.startActivity(intent); true } catch (_: ActivityNotFoundException) { false }
    }

    fun shareObtainium(context: Context) {
        require(configured) { "The OpenCLScope release repository is not configured in this build" }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "OpenCLScope Obtainium configuration")
            putExtra(Intent.EXTRA_TEXT, obtainiumJson())
        }
        context.startActivity(Intent.createChooser(intent, "Share Obtainium configuration"))
    }

    fun openInBrowser(context: Context, url: String): Boolean {
        if (!configured) return false
        val base = sourceUrl()
        if (url != base && !url.startsWith("$base/releases")) return false
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) { false }
    }

    suspend fun check(): GitHubRelease = withContext(Dispatchers.IO) {
        require(configured) { "Release repository is not configured" }
        val s = slug()
        val url = URI("https://api.github.com/repos/$s/releases/latest")
        val connection = url.toURL().openConnection() as HttpsURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "OpenCLScope/${BuildConfig.VERSION_NAME}")
        try {
            require(connection.responseCode == 200) { "GitHub release check HTTP ${connection.responseCode}" }
            val data = ByteArrayOutputStream()
            connection.inputStream.use { stream ->
                val buf = ByteArray(4096)
                while (true) {
                    val n = stream.read(buf)
                    if (n < 0) break
                    require(n > 0 && data.size() + n <= 1024 * 1024) { "GitHub response exceeded safety bound" }
                    data.write(buf, 0, n)
                }
            }
            val obj = JSONObject(data.toString(Charsets.UTF_8.name()))
            val page = obj.getString("html_url")
            require(page.startsWith("${sourceUrl()}/releases/tag/")) { "Unexpected release URL" }
            val assets = obj.optJSONArray("assets")
            var apk: String? = null
            var size = 0L
            if (assets != null) for (i in 0 until assets.length()) {
                val x = assets.getJSONObject(i)
                val name = x.optString("name")
                val candidate = x.optString("browser_download_url")
                if (Regex("(?i).*universal.*\\.apk$").matches(name) && candidate.startsWith("${sourceUrl()}/releases/download/")) {
                    apk = candidate
                    size = x.optLong("size").coerceAtLeast(0)
                    break
                }
            }
            GitHubRelease(obj.getString("tag_name"), obj.optString("published_at"), page, apk, size)
        } finally { connection.disconnect() }
    }
}
