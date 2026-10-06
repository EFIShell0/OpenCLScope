package com.efishell.openclscope

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

internal enum class SharedStorageMode { IMPORT, EXPORT }
internal enum class FileManagerViewMode { LIST, COMPACT, DETAILS, GRID, DENSE_GRID, LARGE_GRID }
internal enum class FileManagerSortMode { NAME_ASC, NAME_DESC, MODIFIED_NEWEST, MODIFIED_OLDEST, CREATED_NEWEST, CREATED_OLDEST }

internal data class SharedStorageRequest(
    val title: String,
    val description: String,
    val mode: SharedStorageMode,
    val allowedExtensions: Set<String>,
    val suggestedFileName: String = "",
    val maxImportBytes: Long = 16L * 1024L * 1024L
)

private data class SharedStorageEntry(val file: File, val createdAt: Long)
private data class SharedStorageListing(val folders: List<SharedStorageEntry>, val files: List<SharedStorageEntry>, val limited: Boolean)

private fun fileCreatedAt(file: File): Long = runCatching {
    java.nio.file.Files.readAttributes(file.toPath(), java.nio.file.attribute.BasicFileAttributes::class.java).creationTime().toMillis()
}.getOrDefault(file.lastModified())

private fun sortFiles(files: MutableList<File>, mode: FileManagerSortMode) {
    val locale = Locale.getDefault()
    when (mode) {
        FileManagerSortMode.NAME_ASC -> files.sortBy { it.name.lowercase(locale) }
        FileManagerSortMode.NAME_DESC -> files.sortByDescending { it.name.lowercase(locale) }
        FileManagerSortMode.MODIFIED_NEWEST -> files.sortWith(compareByDescending<File> { it.lastModified() }.thenBy { it.name.lowercase(locale) })
        FileManagerSortMode.MODIFIED_OLDEST -> files.sortWith(compareBy<File> { it.lastModified() }.thenBy { it.name.lowercase(locale) })
        FileManagerSortMode.CREATED_NEWEST -> files.sortWith(compareByDescending<File> { fileCreatedAt(it) }.thenBy { it.name.lowercase(locale) })
        FileManagerSortMode.CREATED_OLDEST -> files.sortWith(compareBy<File> { fileCreatedAt(it) }.thenBy { it.name.lowercase(locale) })
    }
}

private fun fileManagerSortLabel(mode: FileManagerSortMode): String = when (mode) {
    FileManagerSortMode.NAME_ASC -> "A–Z"
    FileManagerSortMode.NAME_DESC -> "Z–A"
    FileManagerSortMode.MODIFIED_NEWEST -> "Modified · newest first"
    FileManagerSortMode.MODIFIED_OLDEST -> "Modified · oldest first"
    FileManagerSortMode.CREATED_NEWEST -> "Created · newest first"
    FileManagerSortMode.CREATED_OLDEST -> "Created · oldest first"
}

private fun fileManagerViewModeLabel(mode: FileManagerViewMode): String = when (mode) {
    FileManagerViewMode.LIST -> "List"
    FileManagerViewMode.COMPACT -> "Compact list"
    FileManagerViewMode.DETAILS -> "Detailed list"
    FileManagerViewMode.GRID -> "Grid"
    FileManagerViewMode.DENSE_GRID -> "Dense grid"
    FileManagerViewMode.LARGE_GRID -> "Large tiles"
}

private fun fileManagerViewModeIcon(mode: FileManagerViewMode): Int = when (mode) {
    FileManagerViewMode.LIST -> R.drawable.ic_view_list
    FileManagerViewMode.COMPACT -> R.drawable.ic_view_compact
    FileManagerViewMode.DETAILS -> R.drawable.ic_view_details
    FileManagerViewMode.GRID -> R.drawable.ic_view_grid
    FileManagerViewMode.DENSE_GRID -> R.drawable.ic_view_grid_dense
    FileManagerViewMode.LARGE_GRID -> R.drawable.ic_view_tiles_large
}

private fun fileManagerSortIcon(mode: FileManagerSortMode): Int = when (mode) {
    FileManagerSortMode.NAME_ASC -> R.drawable.ic_sort_name_asc
    FileManagerSortMode.NAME_DESC -> R.drawable.ic_sort_name_desc
    FileManagerSortMode.MODIFIED_NEWEST -> R.drawable.ic_sort_modified_newest
    FileManagerSortMode.MODIFIED_OLDEST -> R.drawable.ic_sort_modified_oldest
    FileManagerSortMode.CREATED_NEWEST -> R.drawable.ic_sort_created_newest
    FileManagerSortMode.CREATED_OLDEST -> R.drawable.ic_sort_created_oldest
}

private fun sharedStorageRoot(): File = Environment.getExternalStorageDirectory().canonicalFile

private fun insideRoot(root: File, candidate: File): Boolean {
    val canonicalRoot = root.canonicalFile
    val canonical = candidate.canonicalFile
    val prefix = canonicalRoot.path.trimEnd(File.separatorChar) + File.separator
    return canonical == canonicalRoot || canonical.path.startsWith(prefix)
}

private fun scanDirectory(root: File, directory: File, extensions: Set<String>, includeFiles: Boolean, sort: FileManagerSortMode): SharedStorageListing {
    val canonical = directory.canonicalFile
    require(insideRoot(root, canonical) && canonical.isDirectory && canonical.canRead())
    val children = canonical.listFiles()?.toMutableList() ?: mutableListOf()
    val limited = children.size > 4096
    if (limited) children.subList(4096, children.size).clear()
    sortFiles(children, sort)
    val folders = mutableListOf<SharedStorageEntry>()
    val files = mutableListOf<SharedStorageEntry>()
    children.forEach { child ->
        val safe = runCatching { child.canonicalFile }.getOrNull() ?: return@forEach
        if (!insideRoot(root, safe) || child.absoluteFile.path != safe.path) return@forEach
        if (safe.isDirectory && safe.canRead()) folders += SharedStorageEntry(safe, fileCreatedAt(safe))
        else if (includeFiles && safe.isFile && safe.canRead() && safe.extension.lowercase(Locale.ROOT) in extensions) files += SharedStorageEntry(safe, fileCreatedAt(safe))
    }
    return SharedStorageListing(folders, files, limited)
}

private fun validatedImport(file: File, request: SharedStorageRequest): File {
    val root = sharedStorageRoot()
    val canonical = file.canonicalFile
    require(insideRoot(root, canonical) && file.absoluteFile.path == canonical.path)
    require(canonical.isFile && canonical.canRead())
    require(canonical.extension.lowercase(Locale.ROOT) in request.allowedExtensions)
    require(canonical.length() in 1..request.maxImportBytes)
    return canonical
}

private fun validatedDestination(directory: File, name: String, extensions: Set<String>): File {
    val root = sharedStorageRoot()
    val dir = directory.canonicalFile
    require(insideRoot(root, dir) && dir.isDirectory && dir.canWrite())
    val clean = name.trim()
    require(clean.isNotBlank() && clean.length <= 180 && clean == File(clean).name && !clean.contains('/') && !clean.contains('\\'))
    require(clean.substringAfterLast('.', "").lowercase(Locale.ROOT) in extensions)
    val result = File(dir, clean).canonicalFile
    require(insideRoot(root, result) && result.parentFile?.canonicalFile == dir)
    return result
}

internal fun writeSharedStorageFile(destination: File, bytes: ByteArray) {
    require(bytes.isNotEmpty() && bytes.size <= 64 * 1024 * 1024)
    val dir = destination.parentFile?.canonicalFile ?: error("Destination folder unavailable")
    val temp = File(dir, ".openclscope-${java.util.UUID.randomUUID()}.tmp").canonicalFile
    require(temp.parentFile == dir)
    try {
        FileOutputStream(temp, false).use { output -> output.write(bytes); output.flush(); output.fd.sync() }
        try {
            java.nio.file.Files.move(temp.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            java.nio.file.Files.move(temp.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
        require(destination.isFile && destination.length() == bytes.size.toLong())
    } finally {
        if (temp.exists()) runCatching { temp.delete() }
    }
}

private fun breadcrumbs(root: File, directory: File): List<Pair<String, File>> {
    val result = mutableListOf("Storage" to root)
    val relative = directory.canonicalFile.path.removePrefix(root.canonicalFile.path).trim(File.separatorChar)
    var current = root
    if (relative.isNotBlank()) relative.split(File.separatorChar).filter { it.isNotBlank() }.forEach { part -> current = File(current, part); result += part to current }
    return result
}

@Composable
internal fun OpenCLSharedStorageFileManager(
    request: SharedStorageRequest,
    onDismiss: () -> Unit,
    onImport: suspend (File) -> Result<String> = { Result.failure(IllegalStateException("Import unavailable")) },
    onExport: suspend (File) -> Result<String> = { Result.failure(IllegalStateException("Export unavailable")) }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("file_manager_ui", Context.MODE_PRIVATE) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    val allowed = remember(permissionRevision) { Environment.isExternalStorageManager() }
    val root = remember(allowed) { if (allowed) runCatching { sharedStorageRoot() }.getOrNull() else null }
    var directory by remember(root) { mutableStateOf(root) }
    var search by rememberSaveable(request.title) { mutableStateOf("") }
    var searchExpanded by rememberSaveable(request.title) { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(runCatching { FileManagerViewMode.valueOf(prefs.getString("view", FileManagerViewMode.LIST.name) ?: FileManagerViewMode.LIST.name) }.getOrDefault(FileManagerViewMode.LIST)) }
    var sortMode by remember { mutableStateOf(runCatching { FileManagerSortMode.valueOf(prefs.getString("sort", FileManagerSortMode.NAME_ASC.name) ?: FileManagerSortMode.NAME_ASC.name) }.getOrDefault(FileManagerSortMode.NAME_ASC)) }
    var options by remember { mutableStateOf(false) }
    var listing by remember { mutableStateOf(SharedStorageListing(emptyList(), emptyList(), false)) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val extension = request.allowedExtensions.singleOrNull()?.lowercase(Locale.ROOT)
    val suggestedBase = remember(request.suggestedFileName, extension) {
        val suffix = extension?.let { ".$it" }.orEmpty()
        if (suffix.isNotEmpty() && request.suggestedFileName.endsWith(suffix, true)) request.suggestedFileName.dropLast(suffix.length) else request.suggestedFileName
    }
    var fileName by rememberSaveable(request.title) { mutableStateOf(suggestedBase.take(160)) }
    var overwrite by remember { mutableStateOf<File?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permissionRevision++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(directory, sortMode, refresh, root, request.mode) {
        val target = directory ?: return@LaunchedEffect
        val base = root ?: return@LaunchedEffect
        loading = true
        val result = withContext(Dispatchers.IO) { runCatching { scanDirectory(base, target, request.allowedExtensions, request.mode == SharedStorageMode.IMPORT, sortMode) } }
        result.onSuccess { listing = it; status = null }.onFailure { status = "Folder unavailable" }
        loading = false
    }

    val filteredFolders = remember(listing, search) { listing.folders.filter { search.isBlank() || it.file.name.contains(search, true) } }
    val filteredFiles = remember(listing, search) { listing.files.filter { search.isBlank() || it.file.name.contains(search, true) } }

    fun launchExport(target: File) {
        busy = true
        scope.launch {
            val result = onExport(target)
            status = result.fold({ it }, { it.message ?: "Export failed" })
            busy = false
            if (result.isSuccess) refresh++
        }
    }

    BackHandler { if (options) options = false else onDismiss() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = Color.Black, contentColor = Foreground, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onDismiss, colors = IconButtonDefaults.iconButtonColors(containerColor = OpenCLSurfaceTonal, contentColor = Foreground)) { Icon(painterResource(R.drawable.ic_close), "Close File Manager") }
                    Column(Modifier.weight(1f)) {
                        Text(request.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(request.description, color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { searchExpanded = !searchExpanded }, enabled = allowed, colors = IconButtonDefaults.iconButtonColors(containerColor = AccentContainer, contentColor = AccentSoft)) { Icon(painterResource(R.drawable.ic_search), if (searchExpanded) "Close search" else "Search") }
                    IconButton(onClick = { options = !options }, enabled = allowed, colors = IconButtonDefaults.iconButtonColors(containerColor = OpenCLSurfaceTonal, contentColor = Foreground)) { Icon(painterResource(R.drawable.ic_sort), "View and sort") }
                }
                AnimatedVisibility(searchExpanded && allowed) {
                    OutlinedTextField(search, { search = it.take(128) }, label = { Text("Search") }, placeholder = { Text(if (request.mode == SharedStorageMode.IMPORT) "Folders and supported files" else "Folders") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (!allowed) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(shape = RoundedCornerShape(28.dp), color = OpenCLSurfaceRaised, border = BorderStroke(1.dp, AccentSoft.copy(alpha = .38f)), modifier = Modifier.widthIn(max = 560.dp)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(painterResource(R.drawable.ic_folder), null, tint = AccentSoft, modifier = Modifier.size(36.dp))
                                Text("File Manager needs shared-storage access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("OpenCLScope uses VulkanScope's direct shared-storage File Manager. Android's SAF document picker is not used. Access starts only from this explicit action.", color = OpenCLTextSecondary)
                                Button(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))) }, modifier = Modifier.fillMaxWidth()) { Text("Grant shared-storage access") }
                            }
                        }
                    }
                } else if (root != null && directory != null) {
                    Surface(shape = RoundedCornerShape(18.dp), color = OpenCLSurfaceRaised, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { if (directory != root) directory = directory?.parentFile?.takeIf { insideRoot(root, it) } }, enabled = directory != root) { Icon(painterResource(R.drawable.ic_chevron_left), "Up") }
                            val crumbs = breadcrumbs(root, directory!!)
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                crumbs.takeLast(4).forEachIndexed { index, crumb ->
                                    if (index > 0) Text("/", color = OpenCLTextSecondary)
                                    Text(crumb.first, color = if (crumb.second == directory) AccentSoft else Foreground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { directory = crumb.second }.padding(horizontal = 6.dp, vertical = 5.dp))
                                }
                            }
                            Text(if (request.mode == SharedStorageMode.IMPORT) "${filteredFolders.size} folders · ${filteredFiles.size} files" else "${filteredFolders.size} folders", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (options) {
                        Surface(shape = RoundedCornerShape(22.dp), color = OpenCLSurfaceRaised, border = BorderStroke(1.dp, AccentSoft.copy(alpha = .3f)), modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp)) {
                            val optionsScroll = rememberLazyListState()
                            LazyColumn(state = optionsScroll, modifier = Modifier.fillMaxWidth().desktopVerticalPointerScroll(optionsScroll).hardwareLazyDpad(optionsScroll).focusGroup(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                item { Text("View", fontWeight = FontWeight.Bold); Spacer(Modifier.height(2.dp)) }
                                items(FileManagerViewMode.entries) { mode ->
                                    val selected = mode == viewMode
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) AccentContainer else Color.Transparent).clickable { viewMode = mode; prefs.edit().putString("view", mode.name).apply() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(painterResource(fileManagerViewModeIcon(mode)), null, tint = if (selected) AccentSoft else OpenCLTextSecondary)
                                        Text(fileManagerViewModeLabel(mode), modifier = Modifier.weight(1f))
                                        if (selected) Icon(painterResource(R.drawable.ic_check), null, tint = AccentSoft)
                                    }
                                }
                                item { HorizontalDivider(color = OpenCLOutlineVariant); Text("Sort", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
                                items(FileManagerSortMode.entries) { mode ->
                                    val selected = mode == sortMode
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) AccentContainer else Color.Transparent).clickable { sortMode = mode; prefs.edit().putString("sort", mode.name).apply() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(painterResource(fileManagerSortIcon(mode)), null, tint = if (selected) AccentSoft else OpenCLTextSecondary)
                                        Text(fileManagerSortLabel(mode), modifier = Modifier.weight(1f))
                                        if (selected) Icon(painterResource(R.drawable.ic_check), null, tint = AccentSoft)
                                    }
                                }
                            }
                        }
                    }
                    if (request.mode == SharedStorageMode.EXPORT) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(fileName, { fileName = it.take(160) }, label = { Text("File name") }, singleLine = true, modifier = Modifier.weight(1f), suffix = { extension?.let { Text(".$it") } })
                            Button(onClick = {
                                val name = fileName.trim() + extension?.let { ".$it" }.orEmpty()
                                val target = runCatching { validatedDestination(directory!!, name, request.allowedExtensions) }.getOrElse { status = "Invalid file name or destination"; return@Button }
                                if (target.exists()) overwrite = target else launchExport(target)
                            }, enabled = !busy && fileName.isNotBlank()) { Text("Save") }
                        }
                    }
                    status?.let { Text(it, color = AccentSoft, style = MaterialTheme.typography.labelMedium) }
                    if (listing.limited) Text("Folder contains more than 4096 entries; the bounded File Manager shows the first validated entries only.", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = AccentSoft)
                        else if (viewMode in setOf(FileManagerViewMode.GRID, FileManagerViewMode.DENSE_GRID, FileManagerViewMode.LARGE_GRID)) {
                            val gridState = rememberLazyGridState()
                            val dense = viewMode == FileManagerViewMode.DENSE_GRID
                            val large = viewMode == FileManagerViewMode.LARGE_GRID
                            val minimumCellWidth = when (viewMode) {
                                FileManagerViewMode.DENSE_GRID -> 92.dp
                                FileManagerViewMode.LARGE_GRID -> 228.dp
                                else -> 156.dp
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minimumCellWidth),
                                state = gridState,
                                modifier = Modifier.fillMaxSize().desktopVerticalPointerScroll(gridState).hardwareLazyGridDpad(gridState).focusGroup(),
                                horizontalArrangement = Arrangement.spacedBy(if (dense) 6.dp else 8.dp),
                                verticalArrangement = Arrangement.spacedBy(if (dense) 6.dp else if (large) 10.dp else 8.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                gridItems(filteredFolders, key = { "d:${it.file.path}" }) { entry ->
                                    FileManagerGridCard(entry, true, dense, large, !busy) { directory = entry.file }
                                }
                                if (request.mode == SharedStorageMode.IMPORT) gridItems(filteredFiles, key = { "f:${it.file.path}" }) { entry ->
                                    FileManagerGridCard(entry, false, dense, large, !busy) {
                                        busy = true
                                        scope.launch {
                                            val validation = withContext(Dispatchers.IO) { runCatching { validatedImport(entry.file, request) } }
                                            val result = if (validation.isSuccess) onImport(validation.getOrThrow()) else Result.failure(validation.exceptionOrNull() ?: IllegalStateException("Import validation failed"))
                                            status = result.fold({ it }, { it.message ?: "Import failed" })
                                            busy = false
                                        }
                                    }
                                }
                                if (filteredFolders.isEmpty() && (request.mode == SharedStorageMode.EXPORT || filteredFiles.isEmpty())) item {
                                    Text("No matching folders or files in this location", color = OpenCLTextSecondary, modifier = Modifier.fillMaxWidth().padding(24.dp))
                                }
                            }
                        } else {
                            val state = rememberLazyListState()
                            LazyColumn(state = state, modifier = Modifier.fillMaxSize().desktopVerticalPointerScroll(state).hardwareLazyDpad(state).focusGroup(), verticalArrangement = Arrangement.spacedBy(if (viewMode == FileManagerViewMode.COMPACT) 4.dp else 8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                                items(filteredFolders, key = { "d:${it.file.path}" }) { entry -> FileManagerRow(entry, true, viewMode, !busy) { directory = entry.file } }
                                if (request.mode == SharedStorageMode.IMPORT) items(filteredFiles, key = { "f:${it.file.path}" }) { entry ->
                                    FileManagerRow(entry, false, viewMode, !busy) {
                                        busy = true
                                        scope.launch {
                                            val validation = withContext(Dispatchers.IO) { runCatching { validatedImport(entry.file, request) } }
                                            val result = if (validation.isSuccess) onImport(validation.getOrThrow()) else Result.failure(validation.exceptionOrNull() ?: IllegalStateException("Import validation failed"))
                                            status = result.fold({ it }, { it.message ?: "Import failed" })
                                            busy = false
                                        }
                                    }
                                }
                                if (filteredFolders.isEmpty() && (request.mode == SharedStorageMode.EXPORT || filteredFiles.isEmpty())) item { Text("No matching folders or files in this location", color = OpenCLTextSecondary, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
    overwrite?.let { target ->
        AlertDialog(
            onDismissRequest = { overwrite = null },
            modifier = Modifier.clip(MaterialTheme.shapes.extraLarge).background(OpenCLSurfaceRaised),
            title = { Text("Replace existing file?") },
            text = { Text("${target.name} already exists. Replace it atomically?", color = OpenCLTextSecondary) },
            confirmButton = { Button(onClick = { overwrite = null; launchExport(target) }) { Text("Replace") } },
            dismissButton = { TextButton(onClick = { overwrite = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FileManagerRow(entry: SharedStorageEntry, folder: Boolean, viewMode: FileManagerViewMode, enabled: Boolean, onClick: () -> Unit) {
    val compact = viewMode == FileManagerViewMode.COMPACT || viewMode == FileManagerViewMode.DENSE_GRID
    val detailed = viewMode == FileManagerViewMode.DETAILS || viewMode == FileManagerViewMode.LARGE_GRID
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    Surface(shape = shape, color = OpenCLSurfaceTonal, border = BorderStroke(1.dp, if (folder) OpenCLOutlineVariant else AccentSoft.copy(alpha = .32f)), modifier = Modifier.fillMaxWidth().clip(shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).parityTvBrowseModifier(shape)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = if (compact) 10.dp else 13.dp, vertical = if (compact) 8.dp else 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Surface(shape = RoundedCornerShape(13.dp), color = if (folder) OpenCLSurfaceRaised else AccentContainer, modifier = Modifier.size(if (compact) 38.dp else 46.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(if (folder) R.drawable.ic_folder else R.drawable.ic_code), null, tint = if (folder) Foreground else AccentSoft, modifier = Modifier.size(if (compact) 21.dp else 25.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.file.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!folder) Text("${entry.file.length()} bytes", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall)
                if (detailed) Text("Modified ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(entry.file.lastModified()))}", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), if (folder) "Open folder" else "Select file", tint = AccentSoft)
        }
    }
}

@Composable
private fun FileManagerGridCard(entry: SharedStorageEntry, folder: Boolean, dense: Boolean, large: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (dense) 14.dp else 18.dp)
    Surface(shape = shape, color = OpenCLSurfaceTonal, border = BorderStroke(1.dp, if (folder) OpenCLOutlineVariant else AccentSoft.copy(alpha = .32f)), modifier = Modifier.fillMaxWidth().clip(shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).parityTvBrowseModifier(shape)) {
        if (large) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = if (folder) OpenCLSurfaceRaised else AccentContainer, modifier = Modifier.size(56.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(if (folder) R.drawable.ic_folder else R.drawable.ic_code), null, tint = if (folder) Foreground else AccentSoft, modifier = Modifier.size(32.dp)) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.file.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (folder) "Folder" else "${entry.file.length()} bytes", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("Modified ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(entry.file.lastModified()))}", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(painterResource(R.drawable.ic_chevron_right), if (folder) "Open folder" else "Select file", tint = AccentSoft)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(if (dense) 8.dp else 11.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (dense) 5.dp else 8.dp)) {
                Surface(shape = RoundedCornerShape(if (dense) 12.dp else 16.dp), color = if (folder) OpenCLSurfaceRaised else AccentContainer, modifier = Modifier.size(if (dense) 40.dp else 54.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(painterResource(if (folder) R.drawable.ic_folder else R.drawable.ic_code), null, tint = if (folder) Foreground else AccentSoft, modifier = Modifier.size(if (dense) 24.dp else 32.dp)) }
                }
                Text(entry.file.name, fontWeight = FontWeight.SemiBold, style = if (dense) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!dense) Text(if (folder) "Folder" else "${entry.file.length()} bytes", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall)
                Icon(painterResource(R.drawable.ic_chevron_right), if (folder) "Open folder" else "Select file", tint = AccentSoft, modifier = Modifier.size(if (dense) 18.dp else 22.dp))
            }
        }
    }
}

