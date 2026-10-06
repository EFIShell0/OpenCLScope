package com.efishell.openclscope

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formattedDate(epoch: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epoch))

@Composable
internal fun HistoryPage(revision: Int, onLoad: (CLSnapshot) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<LocalReport>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(revision) { loading = true; rows = ReportStore.list(context); loading = false }
    pendingDelete?.let { name ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("Delete local report?") },
            text = { Text("This deletes one privately stored OpenCL technical report from this device.") },
            confirmButton = { TextButton(onClick = {
                pendingDelete = null
                scope.launch { ReportStore.delete(context, name); rows = ReportStore.list(context) }
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } })
    }
    val visible = remember(rows, query) { rows.filter { it.device.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true) } }

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().desktopVerticalPointerScroll(listState).hardwareLazyDpad(listState), state = listState, contentPadding = parityPageContentPadding(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                item {
                    Text("History", color = Foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Complete successful local captures only. Nothing is uploaded by this feature.", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(query, { query = it }, label = { Text("Search local reports") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("${visible.size} of ${rows.size} stored captures", color = Muted, fontSize = 13.sp)
                    if (loading) EmptyNotice("Loading private history…")
                    if (error.isNotBlank()) EmptyNotice(error)
                }
                if (!loading && rows.isEmpty()) item { EmptyNotice("No local captures yet. Collect a live OpenCL report to create one.") }
                items(visible, key = { it.name }) { row ->
                    ScopeCard(row.device, R.drawable.ic_library) {
                        InfoLine("Captured", formattedDate(row.date))
                        InfoLine("Size", "${row.bytes / 1024} KiB")
                        InfoLine("State", row.status)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    runCatching { ReportStore.read(context, row.name) }
                                        .onSuccess(onLoad).onFailure { error = it.message ?: "Unable to open stored report" }
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Open") }
                            OutlinedButton(onClick = { pendingDelete = row.name }, modifier = Modifier.weight(1f)) { Text("Delete") }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        ScrollBoundaryIndicators(listState, Modifier.matchParentSize().padding(parityScrollHintPadding()))
    }
}

private fun flattenLocal(value: Any?, path: String = "", out: MutableMap<String, String> = linkedMapOf(), depth: Int = 0): Map<String, String> {
    require(depth <= 32 && out.size <= 100000) { "Technical comparison safety limit exceeded" }
    when (value) {
        is JSONObject -> {
            val keys = value.keys().asSequence().toList().sorted()
            if (keys.isEmpty()) out[path] = "{}"
            keys.forEach { key -> flattenLocal(value.get(key), if (path.isBlank()) key else "$path.$key", out, depth + 1) }
        }
        is JSONArray -> {
            if (value.length() == 0) out[path] = "[]"
            for (index in 0 until value.length()) flattenLocal(value.get(index), "$path[$index]", out, depth + 1)
        }
        else -> out[path] = if (value == null || value == JSONObject.NULL) "null" else value.toString()
    }
    return out
}

@Composable
internal fun WatchPage(current: CLSnapshot?, revision: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf<List<LocalReport>>(emptyList()) }
    var referenceName by rememberSaveable { mutableStateOf("") }
    var reference by remember { mutableStateOf<CLSnapshot?>(null) }
    var onlyChanges by rememberSaveable { mutableStateOf(true) }
    var search by rememberSaveable { mutableStateOf("") }
    var maxRows by remember { mutableStateOf(100) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(revision) { history = ReportStore.list(context) }
    val allChanges = remember(current, reference) {
        if (current == null || reference == null) emptyList()
        else runCatching {
            val now = flattenLocal(JSONObject(current.raw))
            val before = flattenLocal(JSONObject(reference!!.raw))
            (now.keys + before.keys).toSortedSet().map { key -> Triple(key, before[key], now[key]) }
        }.getOrElse { emptyList() }
    }
    val changes = remember(allChanges, onlyChanges, search) {
        allChanges.filter { (!onlyChanges || it.second != it.third) &&
            (it.first.contains(search, ignoreCase = true) || it.second.orEmpty().contains(search, ignoreCase = true) || it.third.orEmpty().contains(search, ignoreCase = true)) }
    }
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().desktopVerticalPointerScroll(listState).hardwareLazyDpad(listState), state = listState, contentPadding = parityPageContentPadding(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                item {
                    Text("Watch & compare", color = Foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Compare each canonical query path and result state. Unknown is never treated as false.", color = Muted, fontSize = 13.sp)
                    ScopeCard("Reference capture", R.drawable.ic_compare) {
                        if (history.isEmpty()) Text("Collect at least one live report to enable historical comparison.", color = Muted)
                        history.forEach { row ->
                            OutlinedButton(onClick = {
                                referenceName = row.name
                                error = ""
                                scope.launch { runCatching { ReportStore.read(context, row.name) }.onSuccess { reference = it }.onFailure { error = it.message.orEmpty() } }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text((if (referenceName == row.name) "✓  " else "") + "${formattedDate(row.date)} · ${row.device}", maxLines = 2)
                            }
                        }
                    }
                    if (error.isNotBlank()) EmptyNotice(error)
                    if (reference != null && current != null) {
                        ScopeCard("Technical comparison", R.drawable.ic_graph) {
                            InfoLine("Matching keys", allChanges.count { it.second == it.third }.toString())
                            InfoLine("Different / missing", allChanges.count { it.second != it.third }.toString())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Differences only", modifier = Modifier.weight(1f))
                                Switch(onlyChanges, onCheckedChange = { onlyChanges = it; maxRows = 100 })
                            }
                            OutlinedTextField(search, { search = it; maxRows = 100 }, label = { Text("Search path or value") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Text("${changes.size} matching technical entries", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
                items(changes.take(maxRows), key = { it.first }) { (key, before, now) ->
                    ScopeCard(key, R.drawable.ic_compare) {
                        InfoLine("Reference", before ?: "NOT PRESENT")
                        InfoLine("Current", now ?: "NOT PRESENT")
                    }
                }
                if (changes.size > maxRows) item { OutlinedButton(onClick = { maxRows = (maxRows + 100).coerceAtMost(changes.size) }, modifier = Modifier.fillMaxWidth()) { Text("Load next 100") } }
                item { Spacer(Modifier.height(12.dp)) }
            }
        ScrollBoundaryIndicators(listState, Modifier.matchParentSize().padding(parityScrollHintPadding()))
    }
}

private fun parseNumeric(value: String?): Double? {
    if (value == null) return null
    return if (value.startsWith("0x", true)) value.substring(2).toLongOrNull(16)?.toDouble()
        else value.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
}

@Composable
internal fun MinimumPage(device: CLDevice?) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("minimum_requirements", Context.MODE_PRIVATE) }
    var minimumUnits by rememberSaveable { mutableStateOf(prefs.getString("units", "1") ?: "1") }
    var minimumMemoryMiB by rememberSaveable { mutableStateOf(prefs.getString("mem_mib", "128") ?: "128") }
    var extensionCsv by rememberSaveable { mutableStateOf(prefs.getString("extensions", "") ?: "") }
    val units = minimumUnits.toIntOrNull()?.takeIf { it in 1..65536 }
    val memory = minimumMemoryMiB.toLongOrNull()?.takeIf { it in 1L..(1024L * 1024) }
    val needExtensions = extensionCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct().take(30)
    val checks = listOf(
        Triple("Compute units", "CL_DEVICE_MAX_COMPUTE_UNITS", if (units != null) parseNumeric(device?.value("CL_DEVICE_MAX_COMPUTE_UNITS"))?.let { it >= units } else null),
        Triple("Global memory (MiB)", "CL_DEVICE_GLOBAL_MEM_SIZE", if (memory != null) parseNumeric(device?.value("CL_DEVICE_GLOBAL_MEM_SIZE"))?.let { it >= memory.toDouble() * 1048576.0 } else null)
    ) + needExtensions.map { name ->
        val evidence = device?.properties?.firstOrNull { it.name == "CL_DEVICE_EXTENSIONS" }
        Triple("Extension $name", name, if (evidence?.status == "available") device.extensions.contains(name) else null)
    }
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().desktopVerticalPointerScroll(listState).hardwareLazyDpad(listState), state = listState, contentPadding = parityPageContentPadding(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                item {
                    Text("Analyze minimum", color = Foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(device?.name ?: "No selected OpenCL device", color = Muted)
                    ScopeCard("Requirements", R.drawable.ic_properties) {
                        OutlinedTextField(minimumUnits, { minimumUnits = it.take(6) }, label = { Text("Minimum compute units") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = units == null)
                        OutlinedTextField(minimumMemoryMiB, { minimumMemoryMiB = it.take(8) }, label = { Text("Minimum global memory (MiB)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = memory == null)
                        OutlinedTextField(extensionCsv, { extensionCsv = it.take(2048) }, label = { Text("Required extension names, comma separated") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
                        Button(onClick = {
                            prefs.edit().putString("units", minimumUnits).putString("mem_mib", minimumMemoryMiB).putString("extensions", extensionCsv).apply()
                        }, enabled = units != null && memory != null, modifier = Modifier.fillMaxWidth()) { Text("Save minimum requirements") }
                        OutlinedButton(onClick = {
                            minimumUnits = "1"; minimumMemoryMiB = "128"; extensionCsv = ""
                            prefs.edit().remove("units").remove("mem_mib").remove("extensions").apply()
                        }, modifier = Modifier.fillMaxWidth()) { Text("Reset requirements") }
                    }
                    ScopeCard("Evaluation", R.drawable.ic_analysis) {
                        InfoLine("Met", checks.count { it.third == true }.toString())
                        InfoLine("Unmet", checks.count { it.third == false }.toString())
                        InfoLine("Unknown / invalid", checks.count { it.third == null }.toString())
                        Text("This checks the selected device's reported properties, not performance, conformance or other devices.", color = Muted, fontSize = 13.sp)
                    }
                }
                items(checks, key = { it.first }) { (name, property, passes) ->
                    ScopeCard(name, R.drawable.ic_check) {
                        Text(when (passes) { true -> "MET"; false -> "UNMET"; null -> "UNKNOWN / UNAVAILABLE" }, color = if (passes == true) Accent else Color(0xFFE6C36A), fontWeight = FontWeight.Bold)
                        InfoLine("Evidence", device?.value(property) ?: if (property.startsWith("cl_")) (device?.extensions?.contains(property)?.toString() ?: "Unavailable") else "Unavailable")
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        ScrollBoundaryIndicators(listState, Modifier.matchParentSize().padding(parityScrollHintPadding()))
    }
}

@Composable
internal fun UpdatePreferencesPage(
    openingAnimation: Boolean,
    onOpeningAnimation: (Boolean) -> Unit,
    directUpdates: Boolean,
    onDirectUpdates: (Boolean) -> Unit,
    release: GitHubRelease?,
    status: String,
    checking: Boolean,
    report: CLSnapshot?,
    collecting: Boolean,
    onRefreshRuntime: () -> Unit,
    onCheck: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networkAvailable = LocalValidatedInternetAvailable.current
    var showConsent by remember { mutableStateOf(false) }
    var confirmDownload by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf<UpdateProgress?>(null) }
    var verifiedApk by remember { mutableStateOf<java.io.File?>(null) }
    var transferError by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf(false) }
    if (showConsent) AlertDialog(
        onDismissRequest = { showConsent = false },
        title = { Text("Enable direct update checks?") },
        text = { Text("OpenCLScope will check its configured official GitHub Releases source after the opening animation. No report or hardware data is included. APK installation remains a separate user action.") },
        confirmButton = { TextButton(onClick = { onDirectUpdates(true); showConsent = false }) { Text("Enable") } },
        dismissButton = { TextButton(onClick = { showConsent = false }) { Text("Cancel") } }
    )
    if (confirmDownload && release != null) AlertDialog(
        onDismissRequest = { confirmDownload = false },
        title = { Text("Download and verify update?") },
        text = { Text("OpenCLScope will download the selected universal APK from the configured official GitHub release, enforce the size bound, verify package identity, signing certificate and version, then require a separate installation action.") },
        confirmButton = { TextButton(onClick = {
            confirmDownload = false
            downloading = true
            transferError = ""
            verifiedApk = null
            scope.launch {
                runCatching { DirectUpdater.downloadAndVerify(context, release) { updateProgress = it } }
                    .onSuccess { verifiedApk = it }
                    .onFailure { transferError = it.message ?: "Update transfer failed" }
                downloading = false
            }
        }) { Text("Download") } },
        dismissButton = { TextButton(onClick = { confirmDownload = false }) { Text("Cancel") } }
    )
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().desktopVerticalPointerScroll(listState).hardwareLazyDpad(listState),
            state = listState,
            contentPadding = parityPageContentPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ScopeCard("Update preferences", R.drawable.ic_settings) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Direct GitHub updates", color = Foreground, fontWeight = FontWeight.SemiBold)
                            Text(if (directUpdates) "Enabled · update checks use the configured official OpenCLScope GitHub Releases channel" else "Disabled · recommended when Obtainium manages updates", color = OpenCLTextSecondary, fontSize = 13.sp)
                        }
                        Switch(directUpdates, onCheckedChange = { enabled -> if (enabled) showConsent = true else onDirectUpdates(false) })
                    }
                    Text("Direct GitHub updates are enabled only when the official release repository is configured. When disabled, OpenCLScope performs no startup update check and will not download update APKs. Obtainium can track the same official release source without enabling the built-in updater.", color = Muted, fontSize = 13.sp)
                    InfoLine("Release source", if (ReleaseChannel.configured) ReleaseChannel.sourceUrl() else "Not configured")
                    Button(onClick = onCheck, enabled = directUpdates && ReleaseChannel.configured && !checking && networkAvailable, modifier = Modifier.fillMaxWidth()) { Text(if (checking) "Checking…" else "Check for updates") }
                    Text(status, color = Muted, fontSize = 13.sp)
                    release?.let { result ->
                        InfoLine("Latest release", result.tag)
                        InfoLine("Published", result.published)
                        InfoLine("Universal APK", if (result.universalApk == null) "Not published" else "${result.assetSize / 1024} KiB")
                        OutlinedButton(onClick = { ReleaseChannel.openInBrowser(context, result.page) }, enabled = networkAvailable, modifier = Modifier.fillMaxWidth()) { Text("Open official release") }
                        if (result.universalApk != null) {
                            Button(onClick = { confirmDownload = true }, enabled = !downloading && networkAvailable, modifier = Modifier.fillMaxWidth()) { Text(if (downloading) "Downloading…" else "Download & verify in OpenCLScope") }
                            updateProgress?.let { progress ->
                                InfoLine("Transfer", progress.message)
                                InfoLine("Downloaded", if (progress.total != null && progress.total > 0) "${progress.downloaded} / ${progress.total} bytes" else "${progress.downloaded} bytes")
                            }
                            verifiedApk?.let { apk -> Button(onClick = { DirectUpdater.requestInstall(context, apk) }, modifier = Modifier.fillMaxWidth()) { Text("Install verified APK") } }
                            if (transferError.isNotBlank()) Text(transferError, color = Color(0xFFF2A1A1), fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider(color = OpenCLOutlineVariant)
                    Text("Obtainium", color = Foreground, fontWeight = FontWeight.SemiBold)
                    Text("Obtainium uses the same configured official release source. No fabricated repository URL is shipped.", color = Muted, fontSize = 13.sp)
                    Button(onClick = { ReleaseChannel.openObtainium(context) }, enabled = ReleaseChannel.configured && networkAvailable, modifier = Modifier.fillMaxWidth()) { Text("Add / open in Obtainium") }
                    OutlinedButton(onClick = { ReleaseChannel.shareObtainium(context) }, enabled = ReleaseChannel.configured && networkAvailable, modifier = Modifier.fillMaxWidth()) { Text("Share Obtainium configuration") }
                }
            }
            item {
                ScopeCard("Opening animation", R.drawable.ic_opening_animation_toggle) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("OpenCLScope opening animation", color = Foreground, fontWeight = FontWeight.SemiBold)
                            Text(if (openingAnimation) "Enabled · OpenCLScope logo animation plays before startup collection" else "Disabled · app content can start immediately", color = OpenCLTextSecondary, fontSize = 13.sp)
                        }
                        Switch(openingAnimation, onCheckedChange = onOpeningAnimation)
                    }
                    Text("Applies on the next cold launch. When enabled, OpenCL collection, network observation and the optional automatic update check stay deferred until the opening animation completes.", color = Muted, fontSize = 13.sp)
                }
            }
            item {
                ScopeCard("OpenCL runtime", R.drawable.ic_opencl) {
                    Text("The system OpenCL runtime is the only live capability source. Registry entries are reference metadata and never replace runtime evidence.", color = Muted, fontSize = 13.sp)
                    InfoLine("Collection", if (collecting) "Collecting" else report?.status ?: "Not collected")
                    InfoLine("Loader", report?.loader?.ifBlank { "Unavailable" } ?: "Not collected")
                    InfoLine("Platforms", report?.platforms?.size?.toString() ?: "0")
                    InfoLine("Devices", report?.devices?.size?.toString() ?: "0")
                    if (!report?.message.isNullOrBlank()) InfoLine("Runtime state", report?.message.orEmpty(), report?.status ?: "unknown")
                    Button(onClick = onRefreshRuntime, enabled = !collecting, modifier = Modifier.fillMaxWidth()) { Text(if (collecting) "Collecting…" else "Refresh capabilities") }
                    Text("An inaccessible loader, an empty platform list or an OpenCL error stays explicitly unavailable/error. It is never converted into a claim that the GPU lacks OpenCL hardware support.", color = Muted, fontSize = 13.sp)
                }
            }
        }
        ScrollBoundaryIndicators(listState, Modifier.matchParentSize().padding(parityScrollHintPadding()))
    }
}
