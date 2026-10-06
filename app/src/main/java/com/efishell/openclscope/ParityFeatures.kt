package com.efishell.openclscope

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class OpenCLLibraryVersionInfo(
    val name: String,
    val version: String,
    val role: String,
    val license: String,
    val asset: String
)

internal val OPENCL_SCOPE_LIBRARIES = listOf(
    OpenCLLibraryVersionInfo("AndroidX Core KTX", "1.19.0", "Android application support", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("AndroidX Core SplashScreen", "1.2.0", "Android opening splash integration", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("AndroidX Activity Compose", "1.13.0", "Compose activity integration", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("Compose UI", "1.12.0", "Compose UI runtime", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("Compose Foundation", "1.12.0", "Compose foundation components", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("Compose Animation", "1.12.0", "Compose animation primitives", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("Material 3", "1.5.0-alpha27", "Material 3 and expressive components", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("Lifecycle Runtime Compose", "2.11.0", "Lifecycle-aware Compose state", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("OkHttp", "5.5.0", "Explicit network requests and update transfer", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("ZXing Core", "3.5.4", "Local QR code generation", "Apache License 2.0", "licenses/apache_2_0.md"),
    OpenCLLibraryVersionInfo("Khronos OpenCL API Registry", "bundled cl.xml", "Canonical OpenCL API naming/reference data", "Apache License 2.0", "licenses/opencl_registry.md")
)

@Composable
private fun LibraryLicenseDialog(library: OpenCLLibraryVersionInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember(library.asset) {
        runCatching { context.assets.open(library.asset).bufferedReader().use { it.readText() } }.getOrElse { "License unavailable" }
    }
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${library.name} license", fontWeight = FontWeight.SemiBold, color = Foreground)
                Text(library.license, color = Muted, fontSize = 12.sp)
            }
        },
        text = {
            Box(Modifier.fillMaxWidth().heightIn(max = 390.dp)) {
                Column(Modifier.fillMaxWidth().desktopVerticalPointerScroll(scrollState).hardwareScrollDpad(scrollState).verticalScroll(scrollState).focusable().padding(end = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${library.version} · ${library.role}", color = Muted, fontSize = 13.sp)
                    Text(text, color = Foreground, fontSize = 12.sp)
                }
                ScrollBoundaryIndicators(scrollState, Modifier.matchParentSize().padding(horizontal = 8.dp, vertical = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}


@Composable
internal fun OpenCLInfoPage(report: CLSnapshot?, origin: String, directUpdatesEnabled: Boolean, updateCheckInFlight: Boolean, onCheckForUpdates: () -> Unit) {
    val context = LocalContext.current
    val registrySummary = remember {
        runCatching { JSONObject(context.assets.open("registry/catalog.json").bufferedReader().use { it.readText() }) }.getOrNull()
    }
    val installedAbi = remember(context) { detectOpenCLScopeInstalledAbi(context) }
    val networkAvailable = LocalValidatedInternetAvailable.current
    var license by remember { mutableStateOf<OpenCLLibraryVersionInfo?>(null) }
    license?.let { LibraryLicenseDialog(it) { license = null } }
    PageList("Info", R.drawable.ic_info, false, report) {
        item {
            ScopeCard("Developer", R.drawable.ic_code) {
                ParityIdentityBlock("Semih Boran", "EFI Shell · OpenCLScope developer", R.drawable.ic_person)
                ParityExternalLinkRow("Open GitHub profile", if (networkAvailable) "EFIShell0 · Projects and public profile" else "Unavailable without a validated internet connection", R.drawable.ic_action_github, enabled = networkAvailable) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EFIShell0"))) }
                }
            }
        }
        item {
            ScopeCard("Application", R.drawable.openclscope_scope_wordmark) {
                ParityVersionBlock("OpenCLScope", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString(), BuildConfig.APPLICATION_ID, installedAbi)
                ParityActionButton(
                    title = "Check for updates",
                    subtitle = when {
                        !directUpdatesEnabled -> "Direct GitHub updates are disabled in Settings"
                        !ReleaseChannel.configured -> "Official OpenCLScope release repository is not configured in this build"
                        !networkAvailable -> "Unavailable without a validated internet connection"
                        updateCheckInFlight -> "Checking official release channel…"
                        else -> "Configured official OpenCLScope GitHub release channel"
                    },
                    icon = R.drawable.ic_download,
                    enabled = directUpdatesEnabled && ReleaseChannel.configured && networkAvailable && !updateCheckInFlight,
                    trailingIcon = R.drawable.ic_receive,
                    onClick = onCheckForUpdates
                )
                ParityExternalLinkRow(
                    title = "Open GitHub repository",
                    subtitle = when {
                        !ReleaseChannel.configured -> "Official repository is not configured in this build"
                        !networkAvailable -> "Unavailable without a validated internet connection"
                        else -> "Source, releases and project history"
                    },
                    icon = R.drawable.ic_action_github,
                    enabled = ReleaseChannel.configured && networkAvailable
                ) { ReleaseChannel.openInBrowser(context, ReleaseChannel.sourceUrl()) }
                Text(
                    if (directUpdatesEnabled) "Direct update checks use only the configured official OpenCLScope GitHub release channel. APK download still requires explicit review and confirmation." else "Direct GitHub update checks are disabled. Obtainium can manage updates from the configured official release source without OpenCLScope performing its own update discovery.",
                    color = Muted,
                    fontSize = 13.sp
                )
                InfoLine("Report source", origin)
            }
        }
        item {
            ScopeCard("Libraries", R.drawable.ic_library) {
                Text("Direct application and native library identities are reported from the release's pinned build configuration. Build tools are listed separately and are not presented as runtime libraries.", color = Muted, fontSize = 13.sp)
                OPENCL_SCOPE_LIBRARIES.forEach { item ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoLine(item.name, item.version)
                        Text(item.role, color = Muted, fontSize = 12.sp)
                        Text(item.license, color = OpenCLTextSecondary, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            ParityChevronAffordance("License", "Open ${item.name} license") { license = item }
                        }
                    }
                }
            }
        }
        item {
            ScopeCard("Build toolchain", R.drawable.ic_build) {
                InfoLine("Android Gradle Plugin", "9.4.1")
                InfoLine("Kotlin Compose plugin", "2.4.10")
                InfoLine("Gradle wrapper", "9.7.1")
                InfoLine("Android NDK", "29.0.14206865")
                InfoLine("C++ language level", "C++20")
                InfoLine("CMake minimum", "3.22.1")
                InfoLine("Compile SDK", "37")
                InfoLine("Target SDK", "37")
                InfoLine("Minimum SDK", "31")
            }
        }
        item {
            ScopeCard("Device ABI", R.drawable.ic_cpu) {
                InfoLine("Installed ABI", installedAbi)
                InfoLine("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", ").ifBlank { "Unknown" })
                Text("Installed ABI is the native ABI used by this OpenCLScope installation; supported ABIs are the ABIs reported by Android for the device.", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            ScopeCard("Android", R.drawable.ic_android) {
                InfoLine("Manufacturer", Build.MANUFACTURER.ifBlank { "Unknown" })
                InfoLine("Brand", Build.BRAND.ifBlank { "Unknown" })
                InfoLine("Model", Build.MODEL.ifBlank { "Unknown" })
                InfoLine("Android", Build.VERSION.RELEASE.ifBlank { "Unknown" })
                InfoLine("SDK", Build.VERSION.SDK_INT.toString())
                InfoLine("Security patch", Build.VERSION.SECURITY_PATCH.ifBlank { "Unavailable" })
                InfoLine("Codename", Build.VERSION.CODENAME.ifBlank { "Unknown" })
                InfoLine("Product", Build.PRODUCT.ifBlank { "Unknown" })
                InfoLine("Device", Build.DEVICE.ifBlank { "Unknown" })
                InfoLine("Board", Build.BOARD.ifBlank { "Unknown" })
                InfoLine("Hardware", Build.HARDWARE.ifBlank { "Unknown" })
                InfoLine("Build ID", Build.ID.ifBlank { "Unknown" })
                InfoLine("Incremental", Build.VERSION.INCREMENTAL.ifBlank { "Unknown" })
                InfoLine("Build fingerprint", Build.FINGERPRINT.ifBlank { "Unknown" })
            }
        }
        item {
            ScopeCard("OpenCL registry / query engine", R.drawable.ic_registry) {
                InfoLine("Bundled registry", "Khronos OpenCL API Registry · cl.xml")
                InfoLine("Registry SHA-256", registrySummary?.optString("registrySha256")?.ifBlank { "Unknown" } ?: "Unknown")
                InfoLine("Registry core baseline", registrySummary?.optString("maxCoreFeature")?.removePrefix("CL_VERSION_")?.replace('_', '.')?.let { "OpenCL $it" } ?: "Unknown")
                InfoLine("Platform query catalog", registrySummary?.optJSONArray("platformQueries")?.length()?.toString() ?: "Unknown")
                InfoLine("Device query catalog", registrySummary?.optJSONArray("deviceQueries")?.length()?.toString() ?: "Unknown")
                InfoLine("Registered extensions", registrySummary?.optJSONArray("extensions")?.length()?.toString() ?: "Unknown")
                InfoLine("Runtime loader", report?.loader?.ifBlank { "Unavailable" } ?: "Not collected")
                InfoLine("Enumerated platforms", report?.platforms?.size?.toString() ?: "0")
                InfoLine("Enumerated devices", report?.devices?.size?.toString() ?: "0")
                Text("Registry metadata is build-time/offline. Runtime does not download or parse a remote registry. Registry/reference presence is never treated as runtime capability evidence; unavailable, unknown and error query states remain distinct.", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            ScopeCard("About", R.drawable.ic_info) {
                Text("OpenCLScope is not an official Khronos Group project.", color = AccentSoft, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("OpenCLScope is an OpenCL™ capability and device inspection utility for Android. It reports information exposed by the active OpenCL implementation without converting missing runtime visibility into a hardware-support claim.", color = OpenCLTextSecondary)
                Text("OpenCL™ and the OpenCL logo are trademarks of The Khronos Group Inc.", color = Muted, fontSize = 13.sp)
            }
        }
    }
}

private fun detectOpenCLScopeInstalledAbi(context: Context): String {
    val nativeDir = context.applicationInfo.nativeLibraryDir.orEmpty().lowercase(Locale.ROOT)
    return when {
        nativeDir.contains("arm64") -> "arm64-v8a"
        nativeDir.contains("armeabi-v7a") || nativeDir.endsWith("/arm") -> "armeabi-v7a"
        nativeDir.contains("x86_64") -> "x86_64"
        nativeDir.contains("x86") -> "x86"
        else -> Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
    }
}

internal data class EncyclopediaEntry(val name: String, val category: String, val definition: String, val detail: String)

@Composable
internal fun OpenCLEncyclopediaPage() {
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pageSize = 25
    val entries = remember {
        runCatching {
            val root = JSONObject(context.assets.open("registry/encyclopedia.json").bufferedReader().use { it.readText() })
            val array = root.getJSONArray("entries")
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                EncyclopediaEntry(obj.getString("name"), obj.getString("category"), obj.getString("definition"), obj.optString("detail"))
            }
        }.getOrElse { emptyList() }
    }
    val categories = remember(entries) { listOf("All") + entries.map { it.category }.distinct().sorted() }
    val result = remember(entries, search, category) {
        val q = search.trim()
        entries.asSequence().filter { item ->
            (category == "All" || item.category == category) &&
                (q.isBlank() || item.name.contains(q, true) || item.category.contains(q, true) || item.definition.contains(q, true) || item.detail.contains(q, true))
        }.sortedWith(compareBy<EncyclopediaEntry> { if (it.name.equals(q, true)) 0 else if (it.name.startsWith(q, true)) 1 else 2 }.thenBy { it.name }).toList()
    }
    val pageCount = maxOf(1, (result.size + pageSize - 1) / pageSize)
    LaunchedEffect(search, category, result.size) { page = page.coerceIn(0, pageCount - 1) }
    val paged = result.drop(page * pageSize).take(pageSize)
    PageList("Encyclopedia", R.drawable.ic_book, false, null) {
        item {
            ScopeCard("Encyclopedia", R.drawable.ic_book) {
                Text("Search the bundled OpenCL registry/reference catalog. Registry presence is reference metadata and never runtime support evidence.", color = Muted, fontSize = 13.sp)
                OutlinedTextField(search, { search = it.take(256); page = 0 }, label = { Text("Search OpenCL commands, types, tokens & extensions") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OpenCLExpressiveFilterBar(categories, category, "Category") { category = it; page = 0 }
                InfoLine("Matching entries", result.size.toString())
                InfoLine("Bundled reference entries", entries.size.toString())
            }
        }
        if (result.size > pageSize) item { OpenCLCollectionPager(result.size, page, { page = it }, pageSize) }
        items(paged, key = { "${it.category}:${it.name}" }) { entry ->
            ScopeCard(entry.name, when (entry.category) { "Command" -> R.drawable.ic_self_test; "Extension" -> R.drawable.ic_extensions; "Type" -> R.drawable.ic_registry; else -> R.drawable.ic_book }) {
                StateLabel(entry.category, AccentSoft)
                Text(entry.definition, color = Foreground, fontSize = 14.sp)
                if (entry.detail.isNotBlank()) Text(entry.detail, color = Muted, fontSize = 12.sp)
            }
        }
        if (result.size > pageSize) item { OpenCLCollectionPager(result.size, page, { page = it }, pageSize) }
        if (result.isEmpty()) item { EmptyNotice("No encyclopedia entry matches the current search and category filter.") }
    }
}

private fun flattenAnalysis(value: Any?, path: String = "", out: MutableMap<String, String> = linkedMapOf(), depth: Int = 0): Map<String, String> {
    require(depth <= 32 && out.size <= 100000)
    when (value) {
        is JSONObject -> {
            val keys = value.keys().asSequence().toList().sorted()
            if (keys.isEmpty()) out[path] = "{}"
            keys.forEach { key -> flattenAnalysis(value.get(key), if (path.isBlank()) key else "$path.$key", out, depth + 1) }
        }
        is JSONArray -> {
            if (value.length() == 0) out[path] = "[]"
            for (i in 0 until value.length()) flattenAnalysis(value.get(i), "$path[$i]", out, depth + 1)
        }
        else -> out[path] = if (value == null || value == JSONObject.NULL) "null" else value.toString()
    }
    return out
}

private data class AnalysisDifference(val key: String, val baseline: String?, val current: String?)

private fun analysisDiff(baseline: CLSnapshot, current: CLSnapshot): List<AnalysisDifference> {
    val a = flattenAnalysis(JSONObject(baseline.raw))
    val b = flattenAnalysis(JSONObject(current.raw))
    return (a.keys + b.keys).toSortedSet().mapNotNull { key ->
        val before = a[key]
        val now = b[key]
        if (before == now) null else AnalysisDifference(key, before, now)
    }.take(100000)
}

private fun qualityScore(report: CLSnapshot?): Pair<Int, List<Pair<String, Int>>> {
    if (report == null) return 0 to listOf("No completed collection" to 100)
    val properties = report.properties
    val penalties = buildList {
        if (report.status != "ok") add("Collection status is ${report.status}" to 45)
        if (report.platforms.isEmpty()) add("No platform evidence" to 25)
        if (report.devices.isEmpty()) add("No device evidence" to 25)
        val errors = properties.count { it.status == "error" }
        val unavailable = properties.count { it.status == "unavailable" }
        if (errors > 0) add("$errors property queries returned errors" to minOf(20, 2 + errors / 8))
        if (unavailable > 0) add("$unavailable property queries were unavailable" to minOf(10, 1 + unavailable / 16))
        if (report.durationMs > 15000) add("Collection exceeded 15 seconds" to 5)
    }
    return (100 - penalties.sumOf { it.second }).coerceIn(0, 100) to penalties
}

@Composable
private fun ParityFilterBar(labels: List<String>, selected: String, onSelected: (String) -> Unit) {
    OpenCLExpressiveFilterBar(labels, selected, "Filter", onSelected)
}

@Composable
internal fun AnalysisWorkspacePage(
    report: CLSnapshot?,
    device: CLDevice?,
    onHistory: () -> Unit,
    onWatch: () -> Unit,
    onMinimum: () -> Unit,
    onGraph: () -> Unit,
    onTests: () -> Unit,
    onDatabase: () -> Unit,
    onProfiles: () -> Unit,
    onFormats: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val tabs = listOf("Compare", "Search", "Drivers", "Diagnostics", "Requirements", "Minimums", "Graph", "Presentation", "Raw JSON", "Database", "History", "Watched", "Quality", "Share", "Tests")
    var tab by rememberSaveable { mutableStateOf("Compare") }
    var query by rememberSaveable { mutableStateOf("") }
    var baseline by remember { mutableStateOf<CLSnapshot?>(null) }
    var localHistory by remember { mutableStateOf<List<LocalReport>>(emptyList()) }
    var visible by rememberSaveable { mutableIntStateOf(80) }
    LaunchedEffect(Unit) { localHistory = ReportStore.list(context); baseline = localHistory.firstOrNull()?.let { runCatching { ReportStore.read(context, it.name) }.getOrNull() } }
    val flat = remember(report) { report?.let { flattenAnalysis(JSONObject(it.raw)) }.orEmpty() }
    val searchResults = remember(flat, query) { if (query.isBlank()) emptyList() else flat.entries.filter { it.key.contains(query, true) || it.value.contains(query, true) }.take(5000) }
    val diffs = remember(report, baseline) { if (report != null && baseline != null) analysisDiff(baseline!!, report) else emptyList() }
    val score = remember(report) { qualityScore(report) }
    PageList("Analysis workspace", R.drawable.ic_analysis, false, report) {
        item {
            ScopeCard("Analysis workspace", R.drawable.ic_analysis) {
                Text("Analysis, history, custom minimums, tests, watched evidence and Database comparison stay local unless you explicitly submit or fetch a report. Analysis filters never change the complete technical report.", color = Muted, fontSize = 13.sp)
                ParityFilterBar(tabs, tab) { tab = it; visible = 80 }
            }
        }
        when (tab) {
            "Compare" -> {
                item {
                    ScopeCard("Offline report compare", R.drawable.ic_compare) {
                        InfoLine("Baseline", baseline?.devices?.firstOrNull()?.name ?: "No completed local history")
                        InfoLine("Current", report?.devices?.firstOrNull()?.name ?: "No current report")
                        InfoLine("Evidence changes", diffs.size.toString())
                        Text("Differences are evidence changes only. They are not a device, driver or performance ranking.", color = Muted, fontSize = 13.sp)
                    }
                }
                items(diffs.take(visible), key = { it.key }) { row -> ScopeCard(row.key, R.drawable.ic_compare) { row.baseline?.let { InfoLine("Baseline", it) }; row.current?.let { InfoLine("Current", it) } } }
                if (diffs.size > visible) item { OutlinedButton(onClick = { visible = (visible + 100).coerceAtMost(diffs.size) }, modifier = Modifier.fillMaxWidth()) { Text("Load next 100") } }
            }
            "Search" -> {
                item {
                    ScopeCard("Global OpenCL report search", R.drawable.ic_search) {
                        OutlinedTextField(query, { query = it.take(256); visible = 80 }, label = { Text("Search path or returned value") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        InfoLine("Matches", searchResults.size.toString())
                        Text("Searches the completed evidence model. Registry/reference symbols remain separate and belong in Encyclopedia.", color = Muted, fontSize = 13.sp)
                    }
                }
                items(searchResults.take(visible), key = { it.key }) { row -> ScopeCard(row.key, R.drawable.ic_evidence) { InfoLine("Value", row.value) } }
            }
            "Drivers" -> item {
                ScopeCard("OpenCL runtime paths", R.drawable.ic_opencl) {
                    InfoLine("Loader", report?.loader?.ifBlank { "Unavailable" } ?: "Not collected")
                    InfoLine("Platforms", report?.platforms?.size?.toString() ?: "0")
                    report?.platforms?.take(16)?.forEach { platform -> InfoLine("Platform ${platform.index + 1}", "${platform.vendor} · ${platform.version}") }
                    Text("OpenCLScope does not replace system OpenCL implementations. Reported driver and runtime strings come only from OpenCL queries; no replacement-driver capability is inferred.", color = Muted, fontSize = 13.sp)
                }
            }
            "Diagnostics" -> item {
                ScopeCard("Diagnostic collection", R.drawable.ic_analysis) {
                    InfoLine("Complete collection", report?.let { "${"%.3f".format(Locale.US, it.durationMs / 1000.0)} s (${it.durationMs} ms)" } ?: "Unavailable")
                    InfoLine("Status", report?.status ?: "Not collected")
                    InfoLine("Native result", report?.errorCode?.toString() ?: "Unavailable")
                    InfoLine("Available queries", report?.properties?.count { it.status == "available" }?.toString() ?: "0")
                    InfoLine("Unavailable queries", report?.properties?.count { it.status == "unavailable" }?.toString() ?: "0")
                    InfoLine("Error queries", report?.properties?.count { it.status == "error" }?.toString() ?: "0")
                    Text("Missing evidence stays unavailable/unknown and is never rewritten as unsupported.", color = Muted, fontSize = 13.sp)
                }
            }
            "Requirements" -> item { ScopeCard("Capability requirements", R.drawable.ic_profile) { Text("Evaluate the selected OpenCL device against the local capability checklist and explicit evidence states.", color = Muted); SectionButton("Open Profiles", R.drawable.ic_profile, onProfiles) } }
            "Minimums" -> item { ScopeCard("Custom minimums", R.drawable.ic_properties) { Text("Build and evaluate local minimum requirements. These do not alter report collection or Database payloads.", color = Muted); SectionButton("Analyze minimum", R.drawable.ic_properties, onMinimum) } }
            "Graph" -> item { ScopeCard("Graph explorer", R.drawable.ic_graph) { Text("Visualize returned memory-capacity evidence for the selected OpenCL device.", color = Muted); SectionButton("Open graph", R.drawable.ic_graph, onGraph) } }
            "Presentation" -> item { ScopeCard("Queues & image capability evidence", R.drawable.ic_formats) { Text("OpenCL has no Vulkan Surface/WSI equivalent. The API-specific counterpart exposes command-queue and image-format evidence without fabricating presentation support.", color = Muted); SectionButton("Image formats", R.drawable.ic_formats, onFormats) } }
            "Raw JSON" -> {
                item {
                    ScopeCard("Raw structured technical report", R.drawable.ic_code) {
                        OutlinedTextField(query, { query = it.take(256); visible = 80 }, label = { Text("Search JSON leaves") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        InfoLine("Leaf values", flat.size.toString())
                        OutlinedButton(onClick = { report?.raw?.let { clipboard.setText(AnnotatedString(it)) } }, enabled = report != null, modifier = Modifier.fillMaxWidth()) { Text("Copy complete JSON") }
                    }
                }
                val rows = if (query.isBlank()) flat.entries.take(visible) else searchResults.take(visible)
                items(rows, key = { it.key }) { row -> ScopeCard(row.key, R.drawable.ic_code) { InfoLine("Value", row.value) } }
            }
            "Database" -> item { ScopeCard("OpenCLScope Database lookup", R.drawable.ic_database_browse) { Text("Public list, exact report-ID lookup, local comparison and complete-report submission are explicit user actions.", color = Muted); SectionButton("Open Database", R.drawable.ic_database_submit, onDatabase) } }
            "History" -> item { ScopeCard("Local session history", R.drawable.ic_library) { InfoLine("Stored sessions", localHistory.size.toString()); Text("Completed reports are stored in private app storage with bounded count and size.", color = Muted); SectionButton("Open History", R.drawable.ic_library, onHistory) } }
            "Watched" -> item { ScopeCard("Watched evidence", R.drawable.ic_watch_add) { Text("Watched evidence remains local and bounded. Use Watch & compare to manage the current watch set.", color = Muted); SectionButton("Open watched evidence", R.drawable.ic_compare, onWatch) } }
            "Quality" -> {
                item {
                    ScopeCard("Collection integrity score", R.drawable.ic_shield) {
                        Text(score.first.toString(), color = AccentSoft, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text("This is not OpenCL conformance, performance, certification or a GPU quality ranking. It summarizes only explicit collection anomalies.", color = Muted, fontSize = 13.sp)
                    }
                }
                items(score.second, key = { it.first }) { p -> ScopeCard(p.first, R.drawable.ic_evidence) { InfoLine("Penalty", "-${p.second}") } }
            }
            "Share" -> item {
                ScopeCard("Share & export", R.drawable.ic_share) {
                    Text("Canonical JSON/TXT/HTML export remains complete and preserves unavailable/error evidence. Sharing does not submit to the Database.", color = Muted)
                    OutlinedButton(onClick = { report?.raw?.let { clipboard.setText(AnnotatedString(it)) } }, enabled = report != null, modifier = Modifier.fillMaxWidth()) { Text("Copy complete JSON") }
                }
            }
            "Tests" -> item { ScopeCard("Collection tests", R.drawable.ic_check) { Text("Collection-path checks are kept separate from unexecuted compute benchmarks.", color = Muted); SectionButton("Open tests", R.drawable.ic_check, onTests) } }
        }
    }
}

