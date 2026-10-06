package com.efishell.openclscope

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

internal data class UpdateProgress(val downloaded: Long, val total: Long?, val message: String)

internal object DirectUpdater {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun downloadAndVerify(context: Context, release: GitHubRelease, onProgress: (UpdateProgress) -> Unit): File = withContext(Dispatchers.IO) {
        val asset = release.universalApk ?: error("This release does not publish a universal APK")
        require(ReleaseChannel.configured)
        require(asset.startsWith("${ReleaseChannel.sourceUrl()}/releases/download/"))
        val folder = File(context.cacheDir, "updates")
        require(folder.isDirectory || folder.mkdirs())
        folder.listFiles()?.forEach { if (it.isFile) it.delete() }
        val temp = File(folder, "OpenCLScope-${release.tag.removePrefix("v")}.download")
        val target = File(folder, "OpenCLScope-${release.tag.removePrefix("v")}.apk")
        val request = Request.Builder().url(asset).header("User-Agent", "OpenCLScope/${BuildConfig.VERSION_NAME}").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Update download HTTP ${response.code}")
            val body = response.body
            val length = body.contentLength().takeIf { it >= 0 }
            if (length != null && length > 256L * 1024L * 1024L) error("Update package exceeds the safety limit")
            if (release.assetSize > 0 && length != null && length != release.assetSize) error("Release asset size does not match GitHub metadata")
            var total = 0L
            FileOutputStream(temp).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        total += count
                        if (total > 256L * 1024L * 1024L) error("Update package exceeds the safety limit")
                        output.write(buffer, 0, count)
                        onProgress(UpdateProgress(total, length, "Downloading"))
                    }
                }
                output.fd.sync()
            }
            if (release.assetSize > 0 && total != release.assetSize) error("Downloaded package size does not match the selected release")
        }
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        try {
            verifyPackage(context, target, release.tag.removePrefix("v"))
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
        onProgress(UpdateProgress(target.length(), target.length(), "Verified"))
        target
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptySet()
            if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            info.signatures ?: emptyArray()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun verifyPackage(context: Context, apk: File, expectedVersion: String) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(apk.absolutePath, flags) ?: error("Downloaded file is not a valid Android package")
        if (archive.packageName != context.packageName) error("Downloaded package identity does not match OpenCLScope")
        val installed = pm.getPackageInfo(context.packageName, flags)
        val installedSigners = signerDigests(installed)
        val archiveSigners = signerDigests(archive)
        if (installedSigners.isEmpty() || archiveSigners.isEmpty() || installedSigners.intersect(archiveSigners).isEmpty()) error("Downloaded package signing certificate does not match the installed OpenCLScope build")
        val archiveCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archive.versionCode.toLong()
        }
        val installedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            installed.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            installed.versionCode.toLong()
        }
        if (archiveCode <= installedCode) error("Downloaded package versionCode is not newer than the installed build")
        if ((archive.versionName ?: "") != expectedVersion) error("Downloaded package versionName does not match the selected release")
    }

    fun requestInstall(context: Context, apk: File) {
        require(apk.isFile && apk.length() in 1L..(256L * 1024L * 1024L))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
