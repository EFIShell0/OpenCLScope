package com.efishell.openclscope

import android.content.Intent
import android.os.Bundle
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.util.Locale
import java.io.File

internal val Accent = Color(0xFF3DAE2B)
internal val AccentSoft = Color(0xFF7ED96C)
internal val AccentContainer = Color(0xFF163F18)
private val Lime = AccentSoft
private val Black = Color(0xFF000000)
private val Panel = Color(0xFF101010)
private val Raised = Color(0xFF181516)
private val Border = Color(0xFF494244)
internal val Foreground = Color(0xFFF7F2F3)
internal val Muted = Color(0xFFB6ACAE)
private const val COLLECTION_PAGE_SIZE = 25

private val OpenCLExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
    largeIncreased = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(36.dp),
    extraExtraLarge = RoundedCornerShape(40.dp)
)

private val OpenCLBaseTypography = Typography()
private val OpenCLTypography = Typography(
    displayLarge = OpenCLBaseTypography.displayLarge.copy(textDirection = TextDirection.ContentOrLtr),
    displayMedium = OpenCLBaseTypography.displayMedium.copy(textDirection = TextDirection.ContentOrLtr),
    displaySmall = OpenCLBaseTypography.displaySmall.copy(textDirection = TextDirection.ContentOrLtr),
    headlineLarge = OpenCLBaseTypography.headlineLarge.copy(textDirection = TextDirection.ContentOrLtr),
    headlineMedium = OpenCLBaseTypography.headlineMedium.copy(textDirection = TextDirection.ContentOrLtr),
    headlineSmall = OpenCLBaseTypography.headlineSmall.copy(textDirection = TextDirection.ContentOrLtr),
    titleLarge = OpenCLBaseTypography.titleLarge.copy(textDirection = TextDirection.ContentOrLtr),
    titleMedium = OpenCLBaseTypography.titleMedium.copy(textDirection = TextDirection.ContentOrLtr),
    titleSmall = OpenCLBaseTypography.titleSmall.copy(textDirection = TextDirection.ContentOrLtr),
    bodyLarge = OpenCLBaseTypography.bodyLarge.copy(textDirection = TextDirection.ContentOrLtr),
    bodyMedium = OpenCLBaseTypography.bodyMedium.copy(textDirection = TextDirection.ContentOrLtr),
    bodySmall = OpenCLBaseTypography.bodySmall.copy(textDirection = TextDirection.ContentOrLtr),
    labelLarge = OpenCLBaseTypography.labelLarge.copy(textDirection = TextDirection.ContentOrLtr),
    labelMedium = OpenCLBaseTypography.labelMedium.copy(textDirection = TextDirection.ContentOrLtr),
    labelSmall = OpenCLBaseTypography.labelSmall.copy(textDirection = TextDirection.ContentOrLtr)
)

internal enum class Screen(val label: String, val icon: Int) {
    Overview("Overview", R.drawable.ic_home),
    OpenCL("OpenCL", R.drawable.ic_opencl),
    Devices("Devices", R.drawable.ic_surface),
    Extensions("Extensions", R.drawable.ic_extensions),
    Properties("Properties & Limits", R.drawable.ic_properties),
    Features("Features", R.drawable.ic_features),
    Memory("Memory", R.drawable.ic_memory),
    Queues("Queues", R.drawable.ic_queues),
    Formats("Formats", R.drawable.ic_formats),
    Profiles("Profiles", R.drawable.ic_profile),
    Analysis("Analysis workspace", R.drawable.ic_analysis),
    Files("File Manager", R.drawable.ic_folder),
    History("History", R.drawable.ic_library),
    Watch("Watch", R.drawable.ic_compare),
    Minimum("Minimum", R.drawable.ic_properties),
    Tests("Tests", R.drawable.ic_check),
    Graph("Graph", R.drawable.ic_graph),
    Encyclopedia("Encyclopedia", R.drawable.ic_book),
    Database("Database", R.drawable.ic_library),
    Settings("Settings", R.drawable.ic_settings),
    Info("Info", R.drawable.ic_info)
}

private fun screenTransitionIndex(page: Screen): Int = when (page) {
    Screen.Overview -> 0
    Screen.OpenCL -> 1
    Screen.Devices -> 2
    Screen.Extensions -> 4
    Screen.Features -> 5
    Screen.Memory -> 6
    Screen.Queues -> 7
    Screen.Formats -> 9
    Screen.Properties -> 10
    Screen.Profiles -> 11
    Screen.Encyclopedia -> 12
    Screen.Analysis -> 13
    Screen.Settings -> 14
    Screen.Info -> 15
    Screen.Files -> 16
    Screen.History -> 17
    Screen.Watch -> 18
    Screen.Minimum -> 19
    Screen.Tests -> 20
    Screen.Graph -> 21
    Screen.Database -> 22
}

private fun preferredOpenCLDeviceIndex(snapshot: CLSnapshot): Int {
    val index = snapshot.devices.indexOfFirst { it.type.equals("GPU", ignoreCase = true) || it.type.contains("GPU", ignoreCase = true) }
    return if (index >= 0) index else 0
}

private fun vendorIcon(returnedVendor: String): Int {
    val value = returnedVendor.lowercase(Locale.ROOT)
    return when {
        "qualcomm" in value -> R.drawable.gpu_vendor_qualcomm
        "nvidia" in value -> R.drawable.gpu_vendor_nvidia
        "advanced micro" in value || "amd" in value -> R.drawable.gpu_vendor_amd
        "intel" in value -> R.drawable.gpu_vendor_intel
        "arm" in value -> R.drawable.gpu_vendor_arm
        "imagination" in value || "powervr" in value -> R.drawable.gpu_vendor_imagination
        "samsung" in value -> R.drawable.gpu_vendor_samsung
        "broadcom" in value -> R.drawable.gpu_vendor_broadcom
        "huawei" in value || "hisilicon" in value -> R.drawable.gpu_vendor_huawei
        "vivante" in value -> R.drawable.gpu_vendor_vivante
        "verisilicon" in value -> R.drawable.gpu_vendor_vsi
        else -> R.drawable.gpu_vendor_unknown
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (getSharedPreferences("openclscope_prefs", 0).getBoolean("opening_animation", true)) {
            installSplashScreen()
        } else {
            setTheme(R.style.AppTheme)
        }
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { OpenCLScopeUi(onShare = { text ->
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "OpenCLScope 0.9.0")
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Share OpenCL report"))
        }) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OpenCLScopeUi(onShare: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("openclscope_prefs", 0) }
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(Screen.Overview) }
    var snapshot by remember { mutableStateOf<CLSnapshot?>(null) }
    var collecting by remember { mutableStateOf(true) }
    var origin by remember { mutableStateOf("Live") }
    var deviceSelection by rememberSaveable { mutableIntStateOf(0) }
    var animateLaunch by rememberSaveable { mutableStateOf(prefs.getBoolean("opening_animation", true)) }
    var openingGate by remember { mutableStateOf(!animateLaunch) }
    var directUpdates by rememberSaveable { mutableStateOf(prefs.getBoolean("direct_updates", false)) }
    var settingsPanel by rememberSaveable { mutableStateOf("Menu") }
    var historyRevision by remember { mutableIntStateOf(0) }
    var release by remember { mutableStateOf<GitHubRelease?>(null) }
    var updateStatus by rememberSaveable { mutableStateOf("Release check has not completed") }
    var checkingUpdates by remember { mutableStateOf(false) }
    suspend fun checkUpdates() {
        if (checkingUpdates) return
        if (!ReleaseChannel.configured) { updateStatus = "The official OpenCLScope release repository is not configured"; return }
        checkingUpdates = true
        updateStatus = "Checking the fixed GitHub Releases source…"
        runCatching { ReleaseChannel.check() }.onSuccess {
            release = it
            updateStatus = if (it.tag.removePrefix("v") == BuildConfig.VERSION_NAME) "OpenCLScope is up to date" else "A published release is available for review"
        }.onFailure { updateStatus = "Release check: ${it.message ?: "unavailable"}" }
        checkingUpdates = false
    }
    var storageRequest by remember { mutableStateOf<SharedStorageRequest?>(null) }
    var storageImport by remember { mutableStateOf<(suspend (File) -> Result<String>)?>(null) }
    var storageExport by remember { mutableStateOf<(suspend (File) -> Result<String>)?>(null) }
    fun closeStorageManager() {
        storageRequest = null
        storageImport = null
        storageExport = null
    }
    fun openImportManager() {
        storageImport = { file ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = file.readBytes()
                    require(bytes.size <= 16 * 1024 * 1024)
                    val imported = CLSnapshot.parse(bytes.toString(Charsets.UTF_8))
                    withContext(Dispatchers.Main) {
                        snapshot = imported
                        deviceSelection = preferredOpenCLDeviceIndex(imported)
                        collecting = false
                        origin = "Imported"
                        page = Screen.Overview
                    }
                    "OpenCLScope report imported"
                }
            }
        }
        storageExport = null
        storageRequest = SharedStorageRequest(
            title = "Import OpenCLScope report",
            description = "Choose a complete OpenCLScope JSON report from shared storage.",
            mode = SharedStorageMode.IMPORT,
            allowedExtensions = setOf("json"),
            maxImportBytes = 16L * 1024L * 1024L
        )
    }
    fun openExportManager(format: ExportFormat) {
        val selected = snapshot ?: return
        storageImport = null
        storageExport = { file ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = ReportExport.encode(selected, format).toByteArray(Charsets.UTF_8)
                    writeSharedStorageFile(file, encoded)
                    "${format.extension.uppercase(Locale.ROOT)} report saved"
                }
            }
        }
        storageRequest = SharedStorageRequest(
            title = "Export complete ${format.extension.uppercase(Locale.ROOT)} report",
            description = "Choose a shared-storage folder and file name. Saving is atomic and preserves complete evidence states.",
            mode = SharedStorageMode.EXPORT,
            allowedExtensions = setOf(format.extension.lowercase(Locale.ROOT)),
            suggestedFileName = "OpenCLScope-0.9.0-report.${format.extension.lowercase(Locale.ROOT)}"
        )
    }
    suspend fun collect() {
        collecting = true
        val result = withContext(Dispatchers.IO) {
            runCatching { CLSnapshot.parse(OpenCLNative.collect()) }
                .getOrElse { CLSnapshot.failure("OpenCL collector could not start: ${it.javaClass.simpleName}") }
        }
        snapshot = result
        origin = "Live"
        deviceSelection = preferredOpenCLDeviceIndex(result)
        if (result.status == "ok" && runCatching { ReportStore.persist(context, result) }.getOrDefault(false)) historyRevision++
        collecting = false
    }
    LaunchedEffect(openingGate) {
        if (openingGate) {
            if (directUpdates && ReleaseChannel.configured) scope.launch { checkUpdates() }
            collect()
        }
    }
    val device = snapshot?.devices?.getOrNull(deviceSelection)
    val theme = darkColorScheme(
        background = Black, surface = Panel, surfaceVariant = Color(0xFF211E1F), primary = Accent, onPrimary = Foreground,
        primaryContainer = AccentContainer, onPrimaryContainer = Foreground, secondary = AccentSoft, onSecondary = Foreground,
        secondaryContainer = Color(0xFF172A19), onSecondaryContainer = Foreground, tertiary = AccentSoft,
        onBackground = Foreground, onSurface = Foreground, onSurfaceVariant = Muted, outline = Border, outlineVariant = Color(0xFF2A2527)
    )
    MaterialExpressiveTheme(colorScheme = theme, typography = OpenCLTypography, shapes = OpenCLExpressiveShapes, motionScheme = MotionScheme.expressive()) {
        if (!openingGate) {
            OpenCLScopeOpeningAnimation(onFinished = { openingGate = true })
            return@MaterialExpressiveTheme
        }
        val networkUiState = rememberOpenCLNetworkUiState()
        val chromeCollectionStatus = rememberChromeCollectionStatus(collecting, snapshot)
        val chromeUpdateStatus = rememberChromeUpdateStatus(checkingUpdates, release, updateStatus)
        val chromeSourceLayer = rememberGraphicsLayer()
        val chromeBlurredLayer = rememberGraphicsLayer()
        val chromeBlurRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }
        val chromeRenderEffect = remember(chromeBlurRadiusPx) { AndroidRenderEffect.createBlurEffect(chromeBlurRadiusPx, chromeBlurRadiusPx, Shader.TileMode.CLAMP).asComposeRenderEffect() }
        LaunchedEffect(chromeBlurredLayer, chromeRenderEffect) { chromeBlurredLayer.renderEffect = chromeRenderEffect }
        BackHandler(enabled = page != Screen.Overview || settingsPanel != "Menu") {
            if (page == Screen.Settings && settingsPanel != "Menu") settingsPanel = "Menu" else page = Screen.Overview
        }
        var transientOverlayHeightPx by remember { mutableIntStateOf(0) }
        var bottomNavigationHeightPx by remember { mutableIntStateOf(0) }
        var headerContentInset by remember { mutableStateOf(0.dp) }
        val transientOverlayInset = with(LocalDensity.current) { if (transientOverlayHeightPx > 0) transientOverlayHeightPx.toDp() + 8.dp else 0.dp }
        val navSystemBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val referenceNavigationHeight = (if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 46.dp else PrimaryNavigationHeight) + PrimaryNavigationBottomGap + navSystemBottomInset
        val measuredNavigationHeight = with(LocalDensity.current) { bottomNavigationHeightPx.toDp() }
        val bottomNavigationInset = maxOf(referenceNavigationHeight, measuredNavigationHeight) + PrimaryNavigationContentGap
        CompositionLocalProvider(LocalTransientOverlayContentInset provides transientOverlayInset, LocalValidatedInternetAvailable provides (networkUiState.known && networkUiState.available), LocalBottomNavigationContentInset provides bottomNavigationInset) {
        Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Black,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                OpenCLParityAppHeader(
                    page = page,
                    onBack = { if (page == Screen.Settings && settingsPanel != "Menu") settingsPanel = "Menu" else page = Screen.Overview },
                    onSettings = { settingsPanel = "Menu"; page = Screen.Settings },
                    backdropLayer = chromeBlurredLayer,
                    backdropRootOffset = Offset.Zero
                )
            }
        ) { insets ->
            val topInset = insets.calculateTopPadding()
            SideEffect { headerContentInset = topInset }
            CompositionLocalProvider(LocalAppHeaderContentInset provides topInset) {
            Box(Modifier.fillMaxSize().drawWithContent {
                chromeSourceLayer.record { this@drawWithContent.drawContent() }
                chromeBlurredLayer.record { drawLayer(chromeSourceLayer) }
                drawLayer(chromeSourceLayer)
            }) {
                AnimatedContent(targetState = page, label = "pageTransition", transitionSpec = {
                    val forward = screenTransitionIndex(targetState) > screenTransitionIndex(initialState)
                    if (forward) {
                        slideInHorizontally(animationSpec = spring()) { it / 5 } + fadeIn(animationSpec = spring()) togetherWith
                            slideOutHorizontally(animationSpec = spring()) { -it / 5 } + fadeOut(animationSpec = spring())
                    } else {
                        slideInHorizontally(animationSpec = spring()) { -it / 5 } + fadeIn(animationSpec = spring()) togetherWith
                            slideOutHorizontally(animationSpec = spring()) { it / 5 } + fadeOut(animationSpec = spring())
                    }
                }, modifier = Modifier.fillMaxSize()) { current ->
                    val report = snapshot
                    Column(Modifier.fillMaxSize()) {
                        val multiDevice = report != null && report.devices.size > 1
                        if (multiDevice) {
                            DeviceSelector(report!!.devices, deviceSelection) { deviceSelection = it }
                        }
                        CompositionLocalProvider(LocalAppHeaderContentInset provides if (multiDevice) 0.dp else topInset) {
                        Box(Modifier.weight(1f)) {
                    when (current) {
                        Screen.Overview -> PageList("Overview", R.drawable.ic_home, collecting, report) {
                            vulkanParityOpenCLOverview(report, device, origin, collecting, navigate = { page = it })
                        }
                        Screen.OpenCL -> PageList("OpenCL", R.drawable.ic_opencl, collecting, report) {
                            item {
                                report?.platforms?.forEach { platform ->
                                    ScopeCard("Platform ${platform.index + 1}", R.drawable.ic_opencl) {
                                        InfoLine("Name", platform.name)
                                        InfoLine("Vendor", platform.vendor)
                                        InfoLine("API version", platform.version)
                                        InfoLine("Profile", platform.profile)
                                        InfoLine("Devices", platform.devices.size.toString())
                                        InfoLine("Enumerated extensions", platform.extensions.size.toString())
                                        if (platform.deviceEnumerationStatus == "error") InfoLine("Device query", openCLErrorLabel(platform.deviceEnumerationCode), "error")
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                            item { if (report?.status == "ok") CompactPropertyList("Platform information", report.platforms.flatMap { it.properties }, "CL_PLATFORM_") }
                        }
                        Screen.Devices -> PageList("Devices", R.drawable.ic_surface, collecting, report) {
                            item {
                                if (report != null && report.devices.isNotEmpty()) {
                                    if (device != null) {
                                        ScopeCard("Device identity", R.drawable.ic_surface) {
                                            Image(painterResource(vendorIcon(device.vendor)), "Reported vendor logo", Modifier.size(58.dp), contentScale = ContentScale.Fit)
                                            InfoLine("Name", device.name)
                                            InfoLine("Type", device.type)
                                            InfoLine("Vendor", device.vendor)
                                            InfoLine("OpenCL", device.version)
                                            InfoLine("Driver", device.driverVersion)
                                            InfoLine("Profile", device.profile)
                                            InfoLine("Platform", device.platformName)
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        SectionButton("Features", R.drawable.ic_features) { page = Screen.Features }
                                        SectionButton("Properties & limits", R.drawable.ic_properties) { page = Screen.Properties }
                                        SectionButton("Memory", R.drawable.ic_memory) { page = Screen.Memory }
                                        SectionButton("Queues", R.drawable.ic_queues) { page = Screen.Queues }
                                        SectionButton("Image & formats", R.drawable.ic_formats) { page = Screen.Formats }
                                        SectionButton("Capability profiles", R.drawable.ic_profile) { page = Screen.Profiles }
                                        SectionButton("Collection tests", R.drawable.ic_check) { page = Screen.Tests }
                                        SectionButton("Memory graph", R.drawable.ic_graph) { page = Screen.Graph }
                                    }
                                } else if (report?.status == "ok") {
                                    val firstError = report.platforms.firstOrNull { it.deviceEnumerationStatus == "error" }
                                    EmptyNotice(if (firstError != null) "Device enumeration returned ${openCLErrorLabel(firstError.deviceEnumerationCode)}; no capability claim is inferred." else "No OpenCL device was exposed by the enumerated platform(s).")
                                }
                            }
                        }
                        Screen.Extensions -> ExtensionPage(report, device)
                        Screen.Properties -> PropertyPage("Properties & limits", device, device?.properties.orEmpty(), "")
                        Screen.Features -> PropertyPage("Features", device,
                            device?.properties?.filter { p -> p.name.contains("SUPPORT") || p.name.contains("CAPABILITIES") || p.name.contains("AVAILABLE") || p.name.contains("FP_CONFIG") || p.name.contains("VERSION") }.orEmpty(), "")
                        Screen.Memory -> PropertyPage("Memory", device, device?.properties?.filter { p ->
                            p.name.contains("MEM") || p.name.contains("CACHE") || p.name.contains("ALLOC") || p.name.contains("SVM")
                        }.orEmpty(), "")
                        Screen.Queues -> PropertyPage("Queues", device, device?.properties?.filter { p ->
                            p.name.contains("QUEUE") || p.name.contains("ON_DEVICE") || p.name.contains("EXECUTION_CAPABILITIES") || p.name.contains("MAX_WORK_GROUP") || p.name.contains("MAX_WORK_ITEM")
                        }.orEmpty(), "")
                        Screen.Formats -> FormatsPage(device)
                        Screen.Profiles -> ProfilePage(device)
                        Screen.Analysis -> AnalysisWorkspacePage(
                            report = report, device = device,
                            onHistory = { page = Screen.History },
                            onWatch = { page = Screen.Watch },
                            onMinimum = { page = Screen.Minimum },
                            onGraph = { page = Screen.Graph },
                            onTests = { page = Screen.Tests },
                            onDatabase = { page = Screen.Database },
                            onProfiles = { page = Screen.Profiles },
                            onFormats = { page = Screen.Formats }
                        )
                        Screen.Files -> PageList("File Manager", R.drawable.ic_folder, false, report) {
                            item {
                                ScopeCard("Shared-storage File Manager", R.drawable.ic_folder) {
                                    Text("OpenCLScope uses the same direct shared-storage manager as VulkanScope. Android SAF document pickers are not used.", color = Muted)
                                    SectionButton("Import complete JSON", R.drawable.ic_action_import) { openImportManager() }
                                    SectionButton("Export complete JSON", R.drawable.ic_export) { openExportManager(ExportFormat.JSON) }
                                    SectionButton("Export complete TXT", R.drawable.ic_action_text) { openExportManager(ExportFormat.TXT) }
                                    SectionButton("Export complete HTML", R.drawable.ic_action_html) { openExportManager(ExportFormat.HTML) }
                                }
                            }
                        }
                        Screen.History -> HistoryPage(historyRevision) { stored ->
                            snapshot = stored; deviceSelection = preferredOpenCLDeviceIndex(stored); collecting = false; origin = "Imported"; page = Screen.Overview
                        }
                        Screen.Watch -> WatchPage(report, historyRevision)
                        Screen.Minimum -> MinimumPage(device)
                        Screen.Tests -> TestsPage(report, device)
                        Screen.Graph -> GraphPage(device)
                        Screen.Encyclopedia -> OpenCLEncyclopediaPage()
                        Screen.Database -> DatabasePage(report, origin, collecting)
                        Screen.Settings -> AnimatedContent(
                            targetState = settingsPanel,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                when {
                                    initialState == "Menu" && targetState != "Menu" ->
                                        slideInHorizontally(tween(280)) { it / 8 } + fadeIn(tween(220)) togetherWith
                                            slideOutHorizontally(tween(180)) { -it / 12 } + fadeOut(tween(150))
                                    initialState != "Menu" && targetState == "Menu" ->
                                        slideInHorizontally(tween(240)) { -it / 10 } + fadeIn(tween(200)) togetherWith
                                            slideOutHorizontally(tween(200)) { it / 10 } + fadeOut(tween(150))
                                    else -> fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                                }
                            },
                            label = "settingsSectionTransition"
                        ) { targetPanel ->
                            when (targetPanel) {
                            "Menu" -> PageList("Settings", R.drawable.ic_settings, false, report) {
                                item {
                                    ParitySettingsSectionCards { selection ->
                                        when (selection) {
                                            "Info" -> settingsPanel = "Info"
                                            "Reports & Database" -> settingsPanel = "Reports"
                                            else -> settingsPanel = "Updates"
                                        }
                                    }
                                }
                            }
                            "Info" -> OpenCLInfoPage(report, origin, directUpdates, checkingUpdates) { scope.launch { checkUpdates() } }
                            "Reports" -> PageList("Reports & Database", R.drawable.ic_database_submit, false, report) {
                                item {
                                    ScopeCard("Export complete report", R.drawable.ic_export) {
                                        Button(onClick = { openExportManager(ExportFormat.JSON) }, enabled = report != null, modifier = Modifier.fillMaxWidth()) { Text("Save complete JSON") }
                                        OutlinedButton(onClick = { openExportManager(ExportFormat.TXT) }, enabled = report != null, modifier = Modifier.fillMaxWidth()) { Text("Save complete TXT") }
                                        OutlinedButton(onClick = { openExportManager(ExportFormat.HTML) }, enabled = report != null, modifier = Modifier.fillMaxWidth()) { Text("Save complete HTML") }
                                        OutlinedButton(onClick = { report?.raw?.let(onShare) }, enabled = report != null, modifier = Modifier.fillMaxWidth()) { Text("Share complete JSON") }
                                        OutlinedButton(onClick = { openImportManager() }, modifier = Modifier.fillMaxWidth()) { Text("Load existing JSON") }
                                        Text("Unavailable/unknown/error values stay in every export. Imported data cannot be submitted as new live evidence.", color = Muted, fontSize = 13.sp)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    ScopeCard("OpenCLScope Database", R.drawable.ic_action_database) {
                                        InfoLine("Connection", if (DatabaseClient.configured) "Fixed HTTPS endpoint configured" else "Not configured in this build")
                                        SectionButton("Submit / search / compare", R.drawable.ic_database_submit) { page = Screen.Database }
                                    }
                                }
                            }
                            else -> UpdatePreferencesPage(
                                openingAnimation = animateLaunch,
                                onOpeningAnimation = { animateLaunch = it; prefs.edit().putBoolean("opening_animation", it).apply() },
                                directUpdates = directUpdates,
                                onDirectUpdates = { directUpdates = it; prefs.edit().putBoolean("direct_updates", it).apply() },
                                release = release, status = updateStatus, checking = checkingUpdates,
                                report = report, collecting = collecting,
                                onRefreshRuntime = { scope.launch { collect() } },
                                onCheck = { scope.launch { checkUpdates() } }
                            )
                            }
                        }
                        Screen.Info -> OpenCLInfoPage(report, origin, directUpdates, checkingUpdates) { scope.launch { checkUpdates() } }
                    }
                        }
                        }
                    }
                }
            }
            }
        }
        OpenCLSystemNavigationBackdrop(
            backdropLayer = chromeBlurredLayer,
            backdropRootOffset = Offset.Zero,
            modifier = Modifier.matchParentSize().zIndex(6f)
        )
        OpenCLCompactBottomNavigationBar(
            selectedPage = page,
            onPageSelected = { page = it },
            backdropLayer = chromeBlurredLayer,
            backdropRootOffset = Offset.Zero,
            modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { bottomNavigationHeightPx = it.height }.zIndex(7f)
        )
        OpenCLTransientStatusOverlayHost(
            collectionStatus = chromeCollectionStatus,
            network = networkUiState,
            updateStatus = chromeUpdateStatus,
            onReviewUpdate = { settingsPanel = "Updates"; page = Screen.Settings },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = headerContentInset).onSizeChanged { transientOverlayHeightPx = it.height }.zIndex(11f)
        )
        storageRequest?.let { request ->
            OpenCLSharedStorageFileManager(
                request = request,
                onDismiss = { closeStorageManager() },
                onImport = { file -> storageImport?.invoke(file) ?: Result.failure(IllegalStateException("Import unavailable")) },
                onExport = { file -> storageExport?.invoke(file) ?: Result.failure(IllegalStateException("Export unavailable")) }
            )
        }
        }
        }
    }
}


@Composable
internal fun StateLabel(value: String, color: Color) {
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(value, color = color, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun openCLSectionIcon(title: String, fallback: Int): Int = when {
    title.equals("Developer", true) -> R.drawable.ic_code
    title.equals("Application", true) -> R.drawable.openclscope_scope_wordmark
    title.equals("Libraries", true) -> R.drawable.ic_library
    title.equals("Build toolchain", true) -> R.drawable.ic_build
    title.equals("Device ABI", true) -> R.drawable.ic_cpu
    title.equals("Android", true) || title.equals("Android runtime", true) || title.equals("Operating system", true) -> R.drawable.ic_android
    title.equals("Updates", true) || title.equals("Update preferences", true) || title.contains("Update Preferences", true) -> R.drawable.ic_download
    title.equals("Encyclopedia", true) -> R.drawable.ic_book
    title.equals("How to read encyclopedia entries", true) -> R.drawable.ic_question
    title.equals("Reference search", true) -> R.drawable.ic_search
    title.equals("Analysis workspace", true) -> R.drawable.ic_analysis
    title.equals("Collection integrity score", true) -> R.drawable.ic_shield
    title.equals("Scoring method", true) -> R.drawable.ic_evidence
    title.equals("What this score does not mean", true) -> R.drawable.ic_question
    title.contains("dependency graph", true) || title.equals("Graph legend", true) || title.equals("Graph", true) -> R.drawable.ic_graph
    title.equals("Graph overview", true) -> R.drawable.ic_quick_access_grid
    title.equals("Interactive dependency map", true) -> R.drawable.ic_compass
    title.contains("active self-test", true) || title.equals("Optional active tests", true) || title.equals("Tests", true) -> R.drawable.ic_test
    title.contains("Test result", true) || title.equals("Self-test result", true) -> R.drawable.ic_self_test
    title.equals("Result semantics", true) -> R.drawable.ic_question
    title.contains("requirement", true) -> R.drawable.ic_registry
    title.contains("minimum", true) -> R.drawable.ic_features
    title.contains("profile", true) -> R.drawable.ic_profile
    title.contains("history", true) -> R.drawable.ic_history
    title.contains("watched", true) || title.startsWith("Watch", true) -> R.drawable.ic_watch_add
    title.contains("compare", true) || title.contains("Diff", true) -> R.drawable.ic_compare
    title.equals("OpenCL registry / query engine", true) || title.contains("Raw structured", true) -> R.drawable.ic_registry
    title.equals("Opening animation", true) -> R.drawable.ic_opening_animation_toggle
    title.equals("Export complete report", true) || title.equals("Complete report export", true) -> R.drawable.ic_surface
    title.contains("Database", true) -> R.drawable.ic_action_database
    title.contains("QR", true) -> R.drawable.ic_qr
    title.contains("diagnostic", true) || title.equals("Query overview", true) || title.equals("Collection diagnostics", true) -> R.drawable.ic_evidence
    title.equals("Extension explorer", true) -> R.drawable.ic_extensions
    title.equals("Explore", true) -> R.drawable.ic_compass
    title.contains("quick access", true) -> R.drawable.ic_quick_access_grid
    title.contains("snapshot", true) || title.contains("inspection", true) -> R.drawable.ic_opencl
    title.startsWith("OpenCL", true) || title.contains("runtime", true) -> R.drawable.ic_opencl
    title.contains("feature", true) || title.contains("evaluation", true) -> R.drawable.ic_features
    title.equals("Memory heaps", true) -> R.drawable.ic_memory_heap
    title.equals("Memory types", true) -> R.drawable.ic_memory_type
    title.contains("memory", true) -> R.drawable.ic_memory
    title.contains("queue", true) -> R.drawable.ic_queues
    title.contains("format", true) || title.contains("image", true) -> R.drawable.ic_formats
    title.contains("extension", true) -> R.drawable.ic_extensions
    title.contains("property", true) || title.contains("limit", true) -> R.drawable.ic_properties
    else -> fallback
}

@Composable
private fun OpenCLSectionVectorBadgeIcon(primary: Int, badgeText: String? = null, overlayIcon: Int? = null) {
    Box(Modifier.padding(7.dp).size(27.dp)) {
        Icon(painterResource(primary), contentDescription = null, tint = AccentSoft, modifier = Modifier.align(Alignment.TopStart).size(21.dp))
        Surface(shape = RoundedCornerShape(3.dp), color = Panel, modifier = Modifier.align(Alignment.BottomEnd).border(0.7.dp, AccentSoft, RoundedCornerShape(3.dp))) {
            if (overlayIcon != null) {
                Icon(painterResource(overlayIcon), contentDescription = null, tint = AccentSoft, modifier = Modifier.padding(1.2.dp).size(9.dp))
            } else {
                Text(badgeText.orEmpty(), color = AccentSoft, fontSize = 4.0.sp, lineHeight = 4.3.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 1.dp, vertical = 0.8.dp))
            }
        }
    }
}

@Composable
private fun OpenCLAboutSectionIcon() {
    Box(Modifier.padding(6.dp).size(30.dp)) {
        Image(painterResource(R.drawable.openclscope_scope_wordmark), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.align(Alignment.CenterStart).width(27.dp).height(15.dp))
        Surface(shape = RoundedCornerShape(4.dp), color = Panel, modifier = Modifier.align(Alignment.BottomEnd)) {
            Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = AccentSoft, modifier = Modifier.padding(1.2.dp).size(9.dp))
        }
    }
}

@Composable
internal fun OpenCLSectionHeaderIcon(title: String, sectionIcon: Int) {
    when {
        title.equals("Opening animation", true) -> Box(Modifier.padding(8.dp).size(27.dp), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_opening_animation_toggle), contentDescription = null, tint = AccentSoft, modifier = Modifier.size(20.dp)) }
        title.equals("Encyclopedia", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_book, "REF")
        title.equals("Reference search", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_book, overlayIcon = R.drawable.ic_search)
        title.equals("How to read encyclopedia entries", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_book, overlayIcon = R.drawable.ic_question)
        title.contains("requirement", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_registry, overlayIcon = R.drawable.ic_search)
        title.equals("Current custom evaluation", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_profile, overlayIcon = R.drawable.ic_check)
        title.equals("Saved minimum profiles", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_profile, overlayIcon = R.drawable.ic_save)
        title.contains("minimum builder", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_profile, overlayIcon = R.drawable.ic_add)
        title.contains("dependency graph", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_graph, overlayIcon = R.drawable.ic_search)
        title.equals("Graph overview", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_graph, overlayIcon = R.drawable.ic_quick_access_grid)
        title.equals("Interactive dependency map", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_graph, overlayIcon = R.drawable.ic_compass)
        title.equals("Collection integrity score", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_shield, overlayIcon = R.drawable.ic_check)
        title.equals("Scoring method", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_shield, overlayIcon = R.drawable.ic_evidence)
        title.equals("What this score does not mean", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_shield, overlayIcon = R.drawable.ic_question)
        title.contains("active self-test", true) || title.equals("Optional active tests", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_test, "RUN")
        title.contains("Test result", true) || title.equals("Self-test result", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_self_test, "SUM")
        title.equals("Result semantics", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_test, overlayIcon = R.drawable.ic_question)
        title.equals("OpenCL registry / query engine", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_registry, "REG")
        title.equals("About", true) -> OpenCLAboutSectionIcon()
        title.equals("Extension explorer", true) -> OpenCLSectionVectorBadgeIcon(R.drawable.ic_extensions, overlayIcon = R.drawable.ic_search)
        sectionIcon == R.drawable.openclscope_scope_wordmark -> Image(painterResource(sectionIcon), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.padding(horizontal = 5.dp, vertical = 11.dp).width(32.dp).height(18.dp))
        sectionIcon == R.drawable.ic_android -> Image(painterResource(sectionIcon), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.padding(horizontal = 7.dp, vertical = 12.dp).width(27.dp).height(16.dp))
        else -> Icon(painterResource(sectionIcon), contentDescription = null, tint = AccentSoft, modifier = Modifier.padding(10.dp).size(21.dp))
    }
}

@Composable
internal fun ScopeCard(title: String, icon: Int, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    Surface(color = Raised, shape = shape, modifier = Modifier.fillMaxWidth().parityTvBrowseModifier(shape)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF19351C)) {
                    OpenCLSectionHeaderIcon(title, openCLSectionIcon(title, icon))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Foreground, modifier = Modifier.weight(1f).semantics { heading() })
            }
            HorizontalDivider(color = Color(0xFF2A2527))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun InfoLine(label: String, value: String, status: String = "available") {
    val clipboard = LocalClipboardManager.current
    var copyMenu by remember { mutableStateOf(false) }
    var tvLongPressConsumed by remember(label, value) { mutableStateOf(false) }
    var tvLongPressJob by remember(label, value) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val tvLongPressScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isTelevision = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
        .onPreviewKeyEvent { event ->
            if (!isTelevision) return@onPreviewKeyEvent false
            val nativeKeyCode = event.key.nativeKeyCode
            val activationKey = event.key == Key.DirectionCenter ||
                event.key == Key.Enter ||
                event.key == Key.NumPadEnter ||
                nativeKeyCode == android.view.KeyEvent.KEYCODE_BUTTON_SELECT ||
                nativeKeyCode == android.view.KeyEvent.KEYCODE_BUTTON_A
            if (!activationKey) return@onPreviewKeyEvent false
            when (event.type) {
                KeyEventType.KeyDown -> {
                    if (tvLongPressConsumed) {
                        true
                    } else {
                        val existingJob = tvLongPressJob
                        if (existingJob == null || !existingJob.isActive) {
                            tvLongPressJob = tvLongPressScope.launch {
                                kotlinx.coroutines.delay(550L)
                                copyMenu = true
                                tvLongPressConsumed = true
                            }
                        }
                        false
                    }
                }
                KeyEventType.KeyUp -> {
                    val consumed = tvLongPressConsumed
                    tvLongPressJob?.cancel()
                    tvLongPressJob = null
                    tvLongPressConsumed = false
                    consumed
                }
                else -> false
            }
        }
        .combinedClickable(onClickLabel = "Copy $label", onLongClickLabel = "Open $label copy menu",
            onClick = { clipboard.setText(AnnotatedString(value)) }, onLongClick = { copyMenu = true })
        .padding(vertical = 8.dp, horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label.removePrefix("CL_DEVICE_").removePrefix("CL_PLATFORM_").replace('_', ' '),
            color = Muted, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(if (status == "available") value.ifEmpty { "(empty)" } else "$status${if (value.isNotEmpty()) ": $value" else ""}",
            color = when (status) { "available" -> Foreground; "error" -> Color(0xFFF2A1A1); else -> Lime },
            modifier = Modifier.weight(1.1f), fontSize = 14.sp, softWrap = true,
            style = TextStyle(textDirection = TextDirection.ContentOrLtr))
    }
    HorizontalDivider(color = Border.copy(alpha = 0.35f))
    if (copyMenu) {
        AlertDialog(
            onDismissRequest = { copyMenu = false },
            title = { Text("Copy details") },
            text = { Text("$label: $value", style = TextStyle(textDirection = TextDirection.ContentOrLtr)) },
            confirmButton = { TextButton(onClick = {
                clipboard.setText(AnnotatedString(value))
                copyMenu = false
            }) { Text("Copy value") } },
            dismissButton = { TextButton(onClick = {
                clipboard.setText(AnnotatedString("$label: $value"))
                copyMenu = false
            }) { Text("Copy row") } }
        )
    }
}

@Composable
internal fun SectionButton(title: String, icon: Int, action: () -> Unit) {
    ParitySectionButton(title, icon, action)
}

@Composable
internal fun EmptyNotice(text: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Raised) {
        Text(text, color = Muted, modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }, fontSize = 14.sp)
    }
}

@Composable
private fun DeviceSelector(devices: List<CLDevice>, selected: Int, onSelect: (Int) -> Unit) {
    val headerContentInset = LocalAppHeaderContentInset.current
    val pageTopInset = headerContentInset + PageChromeSeparation
    Column(Modifier.fillMaxWidth().padding(start = 18.dp, top = pageTopInset + 6.dp, end = 18.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("OpenCL device", color = Muted, style = MaterialTheme.typography.labelSmall)
        OpenCLExpressiveFilterBar(
            labels = devices.mapIndexed { index, device -> "Device ${index + 1} · ${device.name.ifBlank { "Unknown" }.take(64)}" },
            selectedIndex = selected,
            title = "OpenCL device",
            onSelected = onSelect
        )
    }
}

@Composable
private fun CompactPropertyList(title: String, properties: List<Capability>, prefix: String) {
    if (properties.isNotEmpty()) ScopeCard(title, R.drawable.ic_properties) {
        properties.filter { it.name.startsWith(prefix) }.forEach { InfoLine(it.name, it.value, it.status) }
    }
}

@Composable
internal fun ScrollBoundaryIndicators(listState: androidx.compose.foundation.lazy.LazyListState, modifier: Modifier = Modifier) {
    val showUp by remember(listState) { derivedStateOf { listState.canScrollBackward } }
    val showDown by remember(listState) { derivedStateOf { listState.canScrollForward } }
    val visibilityKey by remember(listState) {
        derivedStateOf { Triple(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, listState.layoutInfo.totalItemsCount) }
    }
    val visible = rememberScrollIndicatorVisibility(visibilityKey, showUp || showDown, listState.isScrollInProgress)
    ScrollBoundaryIndicatorColumn(showUp, showDown, visible, modifier)
}

@Composable
internal fun ScrollBoundaryIndicators(scrollState: ScrollState, modifier: Modifier = Modifier) {
    val showUp by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }
    val showDown by remember(scrollState) { derivedStateOf { scrollState.value < scrollState.maxValue } }
    val visible = rememberScrollIndicatorVisibility(scrollState.value to scrollState.maxValue, showUp || showDown, scrollState.isScrollInProgress)
    ScrollBoundaryIndicatorColumn(showUp, showDown, visible, modifier)
}

@Composable
private fun rememberScrollIndicatorVisibility(triggerKey: Any, hasScrollableDirection: Boolean, isScrollInProgress: Boolean): Boolean {
    var visible by remember { mutableStateOf(hasScrollableDirection) }
    LaunchedEffect(triggerKey, hasScrollableDirection, isScrollInProgress) {
        if (!hasScrollableDirection) {
            visible = false
        } else {
            visible = true
            delay(if (isScrollInProgress) 1050 else 900)
            visible = false
        }
    }
    return visible
}

@Composable
private fun ScrollBoundaryIndicatorColumn(showUp: Boolean, showDown: Boolean, visible: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.zIndex(10f)) {
        AnimatedVisibility(visible = visible && showUp, modifier = Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            ScrollBoundaryIndicatorBubble(up = true)
        }
        AnimatedVisibility(visible = visible && showDown, modifier = Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            ScrollBoundaryIndicatorBubble(up = false)
        }
    }
}

@Composable
private fun ScrollBoundaryIndicatorBubble(up: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = AccentContainer.copy(alpha = 0.84f),
        tonalElevation = 4.dp,
        shadowElevation = 3.dp
    ) {
        Icon(
            painterResource(if (up) R.drawable.ic_scroll_up else R.drawable.ic_scroll_down),
            contentDescription = null,
            tint = Foreground,
            modifier = Modifier.padding(8.dp).size(24.dp)
        )
    }
}

@Composable
internal fun PageList(title: String, icon: Int, collecting: Boolean, report: CLSnapshot?, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val tvNavigationScope = rememberCoroutineScope()
    val pageFocusRequester = remember { FocusRequester() }
    LaunchedEffect(listState) { runCatching { pageFocusRequester.requestFocus() } }
    val transientOverlayInset = LocalTransientOverlayContentInset.current
    val headerContentInset = LocalAppHeaderContentInset.current
    val bottomNavigationInset = LocalBottomNavigationContentInset.current
    val navigationPadding = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val horizontalStartInset = navigationPadding.calculateLeftPadding(layoutDirection)
    val horizontalEndInset = navigationPadding.calculateRightPadding(layoutDirection)
    val coordinatedTransientOverlayInset by animateDpAsState(
        targetValue = transientOverlayInset,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "coordinatedTopOverlayInset"
    )
    val pageTopInset = headerContentInset + PageChromeSeparation
    val scrollHintTopInset by animateDpAsState(
        targetValue = headerContentInset + transientOverlayInset + 10.dp,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "scrollHintTopInset"
    )
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .desktopVerticalPointerScroll(listState)
                .focusRequester(pageFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val direction = when (event.key.nativeKeyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> FocusDirection.Down
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> FocusDirection.Up
                        else -> null
                    }
                    if (direction != null) {
                        if (focusManager.moveFocus(direction)) return@onPreviewKeyEvent true
                        val info = listState.layoutInfo
                        if (info.totalItemsCount <= 0) return@onPreviewKeyEvent false
                        val target = if (direction == FocusDirection.Down) {
                            ((info.visibleItemsInfo.lastOrNull()?.index ?: listState.firstVisibleItemIndex) + 1).coerceAtMost(info.totalItemsCount - 1)
                        } else {
                            ((info.visibleItemsInfo.firstOrNull()?.index ?: listState.firstVisibleItemIndex) - 1).coerceAtLeast(0)
                        }
                        if (target == listState.firstVisibleItemIndex && !listState.canScrollBackward && direction == FocusDirection.Up) return@onPreviewKeyEvent false
                        if (target == info.totalItemsCount - 1 && !listState.canScrollForward && direction == FocusDirection.Down) return@onPreviewKeyEvent false
                        tvNavigationScope.launch {
                            listState.animateScrollToItem(target)
                            delay(24L)
                            focusManager.moveFocus(direction)
                        }
                        return@onPreviewKeyEvent true
                    }
                    when (event.key.nativeKeyCode) {
                        android.view.KeyEvent.KEYCODE_PAGE_DOWN -> {
                            val info = listState.layoutInfo
                            if (info.totalItemsCount <= 0 || !listState.canScrollForward) false else {
                                val target = (info.visibleItemsInfo.lastOrNull()?.index ?: listState.firstVisibleItemIndex).coerceAtMost(info.totalItemsCount - 1)
                                tvNavigationScope.launch { listState.animateScrollToItem(target) }
                                true
                            }
                        }
                        android.view.KeyEvent.KEYCODE_PAGE_UP -> {
                            if (!listState.canScrollBackward) false else {
                                val target = (listState.firstVisibleItemIndex - (listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) - 1)).coerceAtLeast(0)
                                tvNavigationScope.launch { listState.animateScrollToItem(target) }
                                true
                            }
                        }
                        else -> false
                    }
                }
                .focusGroup(),
            state = listState,
            contentPadding = PaddingValues(
                start = 18.dp + horizontalStartInset,
                top = pageTopInset + coordinatedTransientOverlayInset,
                end = 18.dp + horizontalEndInset,
                bottom = bottomNavigationInset
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (collecting && report == null) item { EmptyNotice("Collecting OpenCL data…") }
            if (report != null && report.status != "ok" && title != "Overview" && title != "Settings" && title != "Info") {
                item { EmptyNotice("${report.status.uppercase(Locale.ROOT)}: ${report.message}") }
            }
            content()
            item { Spacer(Modifier.height(10.dp)) }
        }
        ScrollBoundaryIndicators(
            listState,
            Modifier
                .fillMaxSize()
                .zIndex(10f)
                .padding(
                    start = 12.dp + horizontalStartInset,
                    top = scrollHintTopInset,
                    end = 12.dp + horizontalEndInset,
                    bottom = bottomNavigationInset + 4.dp
                )
        )
    }
}


@Composable
private fun CollectionPager(totalItems: Int, currentPage: Int, onPageChange: (Int) -> Unit, pageSize: Int = COLLECTION_PAGE_SIZE) {
    val pageCount = maxOf(1, (totalItems + pageSize - 1) / pageSize)
    if (pageCount <= 1) return
    val focusManager = LocalFocusManager.current
    var pageField by remember { mutableStateOf(TextFieldValue((currentPage + 1).toString())) }
    var pageFieldFocused by remember { mutableStateOf(false) }
    var suppressFocusCommit by remember { mutableStateOf(false) }
    fun commitPageSelection() {
        val requested = pageField.text.toIntOrNull()
        val resolvedPage = when {
            requested == null -> currentPage + 1
            requested < 1 -> 1
            requested > pageCount -> pageCount
            else -> requested
        }
        pageField = TextFieldValue(resolvedPage.toString())
        if (resolvedPage - 1 != currentPage) onPageChange(resolvedPage - 1)
    }
    fun requestPageChange(targetPage: Int) {
        val bounded = targetPage.coerceIn(0, pageCount - 1)
        suppressFocusCommit = true
        pageField = TextFieldValue((bounded + 1).toString())
        focusManager.clearFocus(force = true)
        pageFieldFocused = false
        suppressFocusCommit = false
        if (bounded != currentPage) onPageChange(bounded)
    }
    LaunchedEffect(currentPage, pageCount, pageFieldFocused) {
        if (!pageFieldFocused) pageField = TextFieldValue((currentPage + 1).coerceIn(1, pageCount).toString())
    }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = OpenCLPagerSurface,
        contentColor = Foreground,
        border = androidx.compose.foundation.BorderStroke(1.dp, OpenCLOutlineVariant.copy(alpha = 0.82f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { if (currentPage > 0) requestPageChange(currentPage - 1) },
                    enabled = currentPage > 0,
                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                        containerColor = AccentContainer,
                        contentColor = Foreground,
                        disabledContainerColor = OpenCLSurfaceTonal,
                        disabledContentColor = Muted
                    )
                ) {
                    Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous page")
                }
                Box(Modifier.width(62.dp).height(48.dp), contentAlignment = Alignment.Center) {
                    OutlinedTextField(
                        value = pageField,
                        onValueChange = { value ->
                            pageField = value.copy(text = value.text.filter(Char::isDigit).take(4))
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commitPageSelection(); focusManager.clearFocus(force = true) }),
                        modifier = Modifier.fillMaxSize().onFocusChanged { state ->
                            val wasFocused = pageFieldFocused
                            pageFieldFocused = state.isFocused
                            if (wasFocused && !state.isFocused && !suppressFocusCommit) commitPageSelection()
                            if (!state.isFocused && pageField.text.isBlank()) pageField = TextFieldValue((currentPage + 1).toString())
                        }
                    )
                    if (!pageFieldFocused) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = (currentPage + 1).coerceIn(1, pageCount),
                                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(130)) },
                                label = "pageNumberTransition"
                            ) { animatedNumber ->
                                Text(animatedNumber.toString(), color = Foreground, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.offset(y = 4.dp))
                            }
                        }
                    }
                }
                Text("/ $pageCount", color = OpenCLTextSecondary, style = MaterialTheme.typography.bodyMedium)
                IconButton(
                    onClick = { if (currentPage + 1 < pageCount) requestPageChange(currentPage + 1) },
                    enabled = currentPage + 1 < pageCount,
                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                        containerColor = AccentContainer,
                        contentColor = Foreground,
                        disabledContainerColor = OpenCLSurfaceTonal,
                        disabledContentColor = Muted
                    )
                ) {
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next page")
                }
            }
        }
    }
}

@Composable
private fun ExtensionPage(report: CLSnapshot?, selectedDevice: CLDevice?) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val found = remember(report, selectedDevice, search, filter) {
        buildList {
            if (filter != "Device") report?.platforms?.forEach { p ->
                p.extensions.forEach { add(Triple(it, "Platform ${p.index + 1}", "${p.index}")) }
            }
            if (filter != "Platform") {
                val devices = if (selectedDevice != null) listOf(selectedDevice) else report?.devices.orEmpty()
                devices.forEach { d -> d.extensions.forEach { add(Triple(it, "Device: ${d.name}", "${d.platformIndex}:${d.index}")) } }
            }
        }.filter { it.first.contains(search.trim(), ignoreCase = true) }
    }
    val pageCount = maxOf(1, (found.size + COLLECTION_PAGE_SIZE - 1) / COLLECTION_PAGE_SIZE)
    LaunchedEffect(search, filter, found.size) { page = page.coerceIn(0, pageCount - 1) }
    val paged = found.drop(page * COLLECTION_PAGE_SIZE).take(COLLECTION_PAGE_SIZE)
    PageList("Extensions", R.drawable.ic_extensions, false, report) {
        item {
            OutlinedTextField(search, { search = it; page = 0 }, label = { Text("Search exact extension names") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OpenCLExpressiveFilterBar(listOf("All", "Device", "Platform"), filter, "Extension scope") { filter = it; page = 0 }
            Text("${found.size} runtime-enumerated entries. Registry listing alone is not evidence of support.", color = Muted, fontSize = 13.sp)
        }
        if (found.size > COLLECTION_PAGE_SIZE) item { CollectionPager(found.size, page, { page = it }) }
        if (report?.status == "ok" && found.isEmpty()) item { EmptyNotice("No matching runtime-enumerated extensions.") }
        items(paged.size) { index ->
            val entry = paged[index]
            ScopeCard(entry.first, R.drawable.ic_extensions) {
                Text(entry.second, color = Muted, fontSize = 13.sp)
                StateLabel("Enumerated", Accent)
            }
        }
    }
}

@Composable
private fun PropertyPage(title: String, device: CLDevice?, items: List<Capability>, initial: String) {
    var query by rememberSaveable(title) { mutableStateOf(initial) }
    var state by rememberSaveable(title) { mutableStateOf("All") }
    var page by rememberSaveable(title) { mutableIntStateOf(0) }
    val visible = remember(items, query, state) {
        items.filter { (state == "All" || it.status == state) && (it.name.contains(query.trim(), ignoreCase = true) || it.value.contains(query.trim(), ignoreCase = true)) }
    }
    val pageCount = maxOf(1, (visible.size + COLLECTION_PAGE_SIZE - 1) / COLLECTION_PAGE_SIZE)
    LaunchedEffect(query, state, visible.size) { page = page.coerceIn(0, pageCount - 1) }
    val paged = visible.drop(page * COLLECTION_PAGE_SIZE).take(COLLECTION_PAGE_SIZE)
    PageList(title, R.drawable.ic_properties, false, null) {
        item {
            Text(device?.name ?: "No OpenCL device selected", color = Muted, fontSize = 14.sp)
            OutlinedTextField(query, { query = it; page = 0 }, label = { Text("Search queried properties") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OpenCLExpressiveFilterBar(listOf("All", "available", "unavailable", "error"), state, "Evidence state") { state = it; page = 0 }
            Text("${visible.size} / ${items.size} query results", color = Muted, fontSize = 13.sp)
        }
        if (visible.size > COLLECTION_PAGE_SIZE) item { CollectionPager(visible.size, page, { page = it }) }
        if (visible.isEmpty()) item { EmptyNotice("No matching query results. Unavailable is not treated as unsupported.") }
        items(paged.size) { index ->
            val item = paged[index]
            ScopeCard(item.name.removePrefix("CL_DEVICE_").replace('_', ' '), R.drawable.ic_properties) {
                Text(item.name, color = Muted, fontSize = 12.sp)
                if (item.status == "available") Text(item.value.ifEmpty { "(empty)" }, color = Foreground, fontSize = 15.sp)
                else {
                    StateLabel(item.status.uppercase(Locale.ROOT), if (item.status == "error") Color(0xFFF2A1A1) else Lime)
                    Text("OpenCL return code: ${item.code}", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

private data class ProfileCheck(val title: String, val property: String, val expected: String)
private val profileChecks = listOf(
    ProfileCheck("Device available", "CL_DEVICE_AVAILABLE", "true"),
    ProfileCheck("Compiler available", "CL_DEVICE_COMPILER_AVAILABLE", "true"),
    ProfileCheck("Compute units present", "CL_DEVICE_MAX_COMPUTE_UNITS", ">0"),
    ProfileCheck("Image support", "CL_DEVICE_IMAGE_SUPPORT", "true"),
    ProfileCheck("Linker available", "CL_DEVICE_LINKER_AVAILABLE", "true"),
    ProfileCheck("Sub-group forward progress", "CL_DEVICE_SUB_GROUP_INDEPENDENT_FORWARD_PROGRESS", "true"),
    ProfileCheck("Non-uniform work-groups", "CL_DEVICE_NON_UNIFORM_WORK_GROUP_SUPPORT", "true"),
    ProfileCheck("Work-group collective functions", "CL_DEVICE_WORK_GROUP_COLLECTIVE_FUNCTIONS_SUPPORT", "true"),
    ProfileCheck("SVM capabilities present", "CL_DEVICE_SVM_CAPABILITIES", ">0")
)

@Composable
private fun ProfilePage(device: CLDevice?) {
    val checks = profileChecks.map { check ->
        val value = device?.value(check.property)
        val passed = when {
            value == null -> null
            check.expected == "true" -> value == "true"
            check.expected == ">0" -> value.removePrefix("0x").toLongOrNull(16)?.let { it > 0 } ?: value.toLongOrNull()?.let { it > 0 }
            else -> null
        }
        Triple(check, value, passed)
    }
    PageList("Profiles", R.drawable.ic_profile, false, null) {
        item {
            Text(device?.name ?: "No selected device", color = Muted)
            ScopeCard("Capability checklist", R.drawable.ic_profile) {
                InfoLine("Met", checks.count { it.third == true }.toString())
                InfoLine("Unmet", checks.count { it.third == false }.toString())
                InfoLine("Unknown / unavailable", checks.count { it.third == null }.toString())
                Text("Informational device checklist, not an OpenCL conformance certification.", color = Muted, fontSize = 13.sp)
            }
        }
        items(checks, key = { it.first.property }) { (check, value, passed) ->
            ScopeCard(check.title, R.drawable.ic_features) {
                StateLabel(when (passed) { true -> "MET"; false -> "UNMET"; null -> "UNKNOWN" }, if (passed == true) Accent else Lime)
                InfoLine(check.property, value ?: "Not available", if (value == null) "unavailable" else "available")
            }
        }
    }
}

@Composable
private fun AnalysisPage(report: CLSnapshot?) {
    PageList("Diagnostics", R.drawable.ic_analysis, false, report) {
        item {
            ScopeCard("Collection result", R.drawable.ic_analysis) {
                InfoLine("Status", report?.status ?: "not collected")
                InfoLine("Message", report?.message ?: "No report")
                InfoLine("Loader", report?.loader?.ifBlank { "Unavailable" } ?: "Unknown")
                InfoLine("Native code", report?.errorCode?.toString() ?: "Unknown")
                InfoLine("Duration", report?.let { "${"%.3f".format(Locale.US, it.durationMs / 1000.0)} s (${it.durationMs} ms)" } ?: "Unknown")
            }
            Spacer(Modifier.height(14.dp))
            val properties = report?.properties.orEmpty()
            ScopeCard("Query integrity", R.drawable.ic_check) {
                InfoLine("Available", properties.count { it.status == "available" }.toString())
                InfoLine("Unavailable", properties.count { it.status == "unavailable" }.toString())
                InfoLine("Error", properties.count { it.status == "error" }.toString())
                InfoLine("Total attempted", properties.size.toString())
                Text("The collector retains explicit query states without guessing unsupported capabilities.", color = Muted, fontSize = 13.sp)
            }
        }
        items(report?.properties?.filter { it.status != "available" }.orEmpty(), key = { it.name + it.code }) { property ->
            ScopeCard(property.name, R.drawable.ic_analysis) {
                InfoLine("State", property.status)
                InfoLine("OpenCL code", property.code.toString())
            }
        }
    }
}

@Composable
private fun EncyclopediaPage() {
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    val catalog = remember {
        runCatching { JSONObject(context.assets.open("registry/catalog.json").bufferedReader().use { it.readText() }) }.getOrNull()
    }
    val keys = remember(catalog) {
        val groups = catalog?.optJSONObject("enums")
        buildList {
            if (groups != null) {
                val iter = groups.keys()
                while (iter.hasNext()) {
                    val key = iter.next()
                    val enumeration = groups.optJSONObject(key) ?: continue
                    val names = enumeration.keys()
                    while (names.hasNext()) {
                        val name = names.next()
                        add(Triple(key, name, enumeration.optString(name)))
                    }
                }
            }
        }
    }
    val visible = keys.filter { it.second.contains(search, ignoreCase = true) || it.first.contains(search, ignoreCase = true) }
    PageList("Encyclopedia", R.drawable.ic_book, false, null) {
        item {
            OutlinedTextField(search, { search = it }, label = { Text("Search official OpenCL tokens") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("${visible.size} entries · Bundled Khronos cl.xml snapshot · Reference data, not device capability evidence", color = Muted, fontSize = 13.sp)
        }
        items(visible.size) { index ->
            val (group, name, value) = visible[index]
            ScopeCard(name, R.drawable.ic_book) {
                Text(group, color = Muted, fontSize = 12.sp)
                Text(value, color = Lime, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FormatsPage(device: CLDevice?) {
    var filter by rememberSaveable { mutableStateOf("") }
    val formats = device?.imageFormats.orEmpty()
        .filter { it.imageType.contains(filter, ignoreCase = true) || it.flags.contains(filter, ignoreCase = true) || it.formats.any { format ->
            format.order.contains(filter, ignoreCase = true) || format.dataType.contains(filter, ignoreCase = true)
        } }
    PageList("Image formats", R.drawable.ic_formats, false, null) {
        item {
            Text(device?.name ?: "No selected OpenCL device", color = Muted)
            OutlinedTextField(filter, { filter = it }, label = { Text("Search image types, flags & formats") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            ScopeCard("Query coverage", R.drawable.ic_formats) {
                InfoLine("Combinations", device?.imageFormats?.size?.toString() ?: "0")
                InfoLine("Returned format pairs", device?.imageFormats?.sumOf { it.formats.size }?.toString() ?: "0")
                Text("Exact order / channel type pairs are queried through clGetSupportedImageFormats for a single-device context.", color = Muted, fontSize = 13.sp)
            }
        }
        if (formats.isEmpty()) item { EmptyNotice("No image-format query results for this selection.") }
        items(formats.size) { index ->
            val group = formats[index]
            ScopeCard(group.imageType.removePrefix("CL_MEM_OBJECT_"), R.drawable.ic_formats) {
                InfoLine("Access", group.flags)
                InfoLine("Status", group.status)
                if (group.status != "available") InfoLine("OpenCL code", group.code.toString())
                if (group.status == "available") {
                    if (group.formats.isEmpty()) Text("No formats returned for this query", color = Muted, fontSize = 13.sp)
                    group.formats.forEach { format -> InfoLine(format.order, format.dataType) }
                }
            }
        }
    }
}

@Composable
private fun TestsPage(report: CLSnapshot?, device: CLDevice?) {
    val results = listOf(
        Triple("Platform enumeration", report?.let { if (it.status == "ok") "PASS" else if (it.status == "unavailable") "UNAVAILABLE" else "ERROR" } ?: "PENDING", "clGetPlatformIDs"),
        Triple("Device enumeration", device?.let { "PASS" } ?: if (report?.status == "ok") "NO DEVICE" else "UNAVAILABLE", "clGetDeviceIDs"),
        Triple("Device identity", device?.let { if (it.name.isNotEmpty() && it.version.isNotEmpty()) "PASS" else "INCOMPLETE" } ?: "UNAVAILABLE", "clGetDeviceInfo"),
        Triple("Image format collection", device?.imageFormats?.let { if (it.any { item -> item.status == "available" }) "PASS" else it.firstOrNull()?.status?.uppercase(Locale.ROOT) ?: "UNAVAILABLE" } ?: "UNAVAILABLE", "clGetSupportedImageFormats"),
        Triple("Compute kernel execution", "NOT RUN", "No unverified benchmark is presented as a passing test in version 0.9.0")
    )
    PageList("Tests", R.drawable.ic_check, false, report) {
        item { EmptyNotice("These are actual collection-path checks. Kernel execution and throughput benchmarking have not been performed.") }
        items(results, key = { it.first }) { result ->
            ScopeCard(result.first, R.drawable.ic_check) {
                StateLabel(result.second, if (result.second == "PASS") Accent else Lime)
                Text(result.third, color = Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun GraphPage(device: CLDevice?) {
    val metrics = listOf(
        "Global memory" to "CL_DEVICE_GLOBAL_MEM_SIZE",
        "Maximum allocation" to "CL_DEVICE_MAX_MEM_ALLOC_SIZE",
        "Global cache" to "CL_DEVICE_GLOBAL_MEM_CACHE_SIZE",
        "Local memory" to "CL_DEVICE_LOCAL_MEM_SIZE",
        "Constant buffer" to "CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE"
    ).mapNotNull { (label, key) ->
        device?.value(key)?.toLongOrNull()?.takeIf { it >= 0 }?.let { Triple(label, key, it) }
    }
    val max = metrics.maxOfOrNull { it.third }?.takeIf { it > 0 } ?: 1L
    PageList("Memory graph", R.drawable.ic_graph, false, null) {
        item {
            Text(device?.name ?: "No selected device", color = Muted)
            ScopeCard("Reported capacities", R.drawable.ic_graph) {
                Text("Scaled to the largest returned value; not a performance benchmark.", color = Muted, fontSize = 13.sp)
                if (metrics.isEmpty()) EmptyNotice("No measured capacity values available")
                metrics.forEach { (label, key, value) ->
                    Text(label, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(8.dp)).background(Raised)) {
                        Box(Modifier.fillMaxWidth((value.toDouble() / max.toDouble()).toFloat().coerceIn(0f, 1f))
                            .height(12.dp).clip(RoundedCornerShape(8.dp)).background(Accent))
                    }
                    Text("${"%.2f".format(Locale.US, value.toDouble() / 1_048_576.0)} MiB · $key", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}



private data class TechnicalDifference(val path: String, val local: String?, val remote: String?)

private fun flattenTechnicalJson(value: Any?, path: String = "", leaves: MutableMap<String, String> = linkedMapOf(), depth: Int = 0): Map<String, String> {
    require(depth <= 32 && leaves.size <= 100000) { "Report exceeds safe comparison complexity" }
    when (value) {
        is JSONObject -> {
            val keys = value.keys().asSequence().toList().sorted()
            if (keys.isEmpty()) leaves[path] = "{}"
            keys.forEach { key -> flattenTechnicalJson(value.get(key), if (path.isEmpty()) key else "$path.$key", leaves, depth + 1) }
        }
        is JSONArray -> {
            if (value.length() == 0) leaves[path] = "[]"
            for (i in 0 until value.length()) flattenTechnicalJson(value.get(i), "$path[$i]", leaves, depth + 1)
        }
        else -> leaves[path] = when (value) {
            null, JSONObject.NULL -> "null"
            else -> value.toString()
        }
    }
    return leaves
}

@Composable
private fun DatabasePage(report: CLSnapshot?, origin: String, collecting: Boolean) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("Database operations require explicit user action.") }
    var receipt by remember { mutableStateOf<ReportReceipt?>(null) }
    var mode by rememberSaveable { mutableStateOf("Database list") }
    var reportId by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var summaries by remember { mutableStateOf<List<PublicReport>>(emptyList()) }
    var cursor by remember { mutableStateOf<ReportCursor?>(null) }
    var cursorExhausted by remember { mutableStateOf(false) }
    var remote by remember { mutableStateOf<JSONObject?>(null) }
    var remoteLabel by rememberSaveable { mutableStateOf("") }
    var differences by remember { mutableStateOf<List<TechnicalDifference>>(emptyList()) }
    var differencesOnly by rememberSaveable { mutableStateOf(true) }
    var differenceSearch by rememberSaveable { mutableStateOf("") }
    var diffVisible by remember { mutableIntStateOf(100) }
    val ready = DatabaseClient.eligible(report, origin, collecting)
    val networkAvailable = LocalValidatedInternetAvailable.current
    val publicUrl = receipt?.id?.let(DatabaseClient::permalink)

    fun fetchRemote(id: String) {
        if (loading) return
        loading = true
        message = "Retrieving the complete public report…"
        scope.launch {
            val result = runCatching { DatabaseClient.fetch(context, id) }
            result.onSuccess { received ->
                remote = received
                remoteLabel = received.optString("id")
                diffVisible = 100
                differences = runCatching {
                    val local = if (ready && report != null) flattenTechnicalJson(JSONObject(report.raw)) else emptyMap()
                    val target = flattenTechnicalJson(received.getJSONObject("technicalReport"))
                    (local.keys + target.keys).toSortedSet().map { key -> TechnicalDifference(key, local[key], target[key]) }
                }.getOrElse { emptyList() }
                message = if (ready) "Database report loaded · ${differences.size} technical leaves compared" else "Database report loaded; local live comparison is unavailable"
            }.onFailure { message = it.message ?: "Database report retrieval failed" }
            loading = false
        }
    }

    fun fetchPage(reset: Boolean) {
        if (loading) return
        if (!reset && cursorExhausted) { message = "No more database reports"; return }
        loading = true
        message = "Reading the public database index…"
        val before = if (reset) null else cursor
        scope.launch {
            val result = runCatching { DatabaseClient.list(context, before) }
            result.onSuccess { page ->
                summaries = if (reset) page.reports else (summaries + page.reports).distinctBy { it.id }.take(500)
                cursor = page.nextCursor
                cursorExhausted = page.nextCursor == null
                message = "${summaries.size} reports loaded; server submission timestamps retained"
            }.onFailure { message = it.message ?: "Database list could not be loaded" }
            loading = false
        }
    }

    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { if (!submitting) confirmSubmit = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_shield), contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
                    Text("Submit complete OpenCL report?")
                }
            },
            text = { Text("Submit every technical platform, device, property, status, extension and image-format result through the configured HTTPS Database. This action is public and cannot be selectively redacted. No report is uploaded automatically.") },
            confirmButton = {
                TextButton(onClick = {
                    if (!ready || submitting || !DatabaseClient.configured || report == null) return@TextButton
                    confirmSubmit = false
                    submitting = true
                    message = "Submitting complete technical report…"
                    scope.launch {
                        val result = runCatching { DatabaseClient.submit(context, report) }
                        result.onSuccess {
                            receipt = it
                            message = "${it.status.uppercase(Locale.ROOT)} · ${it.id} · ${it.submittedAt}"
                        }.onFailure { message = it.message ?: "Submission failed" }
                        submitting = false
                    }
                }, enabled = !submitting && ready && networkAvailable && DatabaseClient.configured) { Text("Submit complete report") }
            },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Cancel") } }
        )
    }

    val visibleDiff = differences.filter { entry ->
        (!differencesOnly || entry.local != entry.remote) &&
            (entry.path.contains(differenceSearch, ignoreCase = true) ||
                entry.local.orEmpty().contains(differenceSearch, ignoreCase = true) ||
                entry.remote.orEmpty().contains(differenceSearch, ignoreCase = true))
    }
    PageList("Reports & Database", R.drawable.ic_library, false, null) {
        item {
            ScopeCard("Connection & privacy", R.drawable.ic_shield) {
                InfoLine("Build endpoint", if (DatabaseClient.configured) "Configured fixed HTTPS origin" else "Not configured")
                InfoLine("Connection", if (networkAvailable) "Validated Internet" else "No validated Internet")
                InfoLine("Data origin", origin)
                InfoLine("Complete live report", if (ready) "Ready" else "Not ready")
                Text("Only a complete live OpenCLScope 0.9.0 report can be submitted. An imported report is not re-submitted as fresh device evidence.", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            ScopeCard("Publish complete report", R.drawable.ic_upload) {
                Button(onClick = { confirmSubmit = true }, enabled = ready && !submitting && DatabaseClient.configured && networkAvailable,
                    modifier = Modifier.fillMaxWidth()) { Text(if (submitting) "Submitting…" else "Submit complete report") }
                if (receipt != null) {
                    InfoLine("Report ID", receipt!!.id)
                    InfoLine("Server time", receipt!!.submittedAt)
                    InfoLine("Result", receipt!!.status)
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(receipt!!.id)) }, modifier = Modifier.fillMaxWidth()) { Text("Copy report ID") }
                    if (publicUrl != null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            OpenCLQrCode(publicUrl, Modifier.size(190.dp))
                        }
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(publicUrl)) }, modifier = Modifier.fillMaxWidth()) { Text("Copy public report link") }
                    }
                }
                Text(if (DatabaseClient.configured) "Explicit submission only. Public reports can be accessed through the new Database." else "The separate Database must first be deployed. Supply the new HTTPS endpoint at build time; no placeholder server is contacted.", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            ScopeCard("Search public reports", R.drawable.ic_search) {
                OpenCLExpressiveFilterBar(listOf("Database list", "Report ID"), mode, "Lookup mode") { mode = it }
                if (mode == "Database list") {
                    Button(onClick = { fetchPage(true) }, enabled = !loading && DatabaseClient.configured && networkAvailable, modifier = Modifier.fillMaxWidth()) { Text("Load / refresh public reports") }
                    OutlinedButton(onClick = { fetchPage(false) }, enabled = !loading && !cursorExhausted && cursor != null && DatabaseClient.configured && networkAvailable, modifier = Modifier.fillMaxWidth()) { Text("Load next 25 reports") }
                } else {
                    OutlinedTextField(reportId, onValueChange = { reportId = it.take(64) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, label = { Text("64-character SHA-256 report ID") })
                    Button(onClick = { fetchRemote(reportId.trim().lowercase(Locale.ROOT)) },
                        enabled = !loading && reportId.trim().matches(Regex("[a-fA-F0-9]{64}")) && DatabaseClient.configured && networkAvailable,
                        modifier = Modifier.fillMaxWidth()) { Text("Fetch exact report") }
                }
            }
            Spacer(Modifier.height(12.dp))
            ScopeCard("Database status", R.drawable.ic_evidence) { Text(message, color = Muted, fontSize = 13.sp) }
        }
        if (mode == "Database list") {
            items(summaries, key = { it.id }) { entry ->
                ScopeCard(entry.deviceName.ifBlank { "Unnamed OpenCL device" }, R.drawable.ic_cpu) {
                    Image(painterResource(vendorIcon(entry.vendor)), "Reported vendor logo", Modifier.size(43.dp), contentScale = ContentScale.Fit)
                    InfoLine("Vendor", entry.vendor)
                    InfoLine("Platform", entry.platformName)
                    InfoLine("OpenCL", entry.openclVersion)
                    InfoLine("Driver", entry.driverVersion)
                    InfoLine("Submitted", entry.submittedAt)
                    InfoLine("Report ID", entry.id)
                    OutlinedButton(onClick = { reportId = entry.id; fetchRemote(entry.id) }, enabled = !loading && networkAvailable,
                        modifier = Modifier.fillMaxWidth()) { Text("Open / compare complete report") }
                }
            }
        }
        if (remote != null) item {
            ScopeCard("Loaded Database report", R.drawable.ic_download) {
                InfoLine("Report ID", remoteLabel)
                InfoLine("Submitted", remote!!.optString("submittedAt"))
                InfoLine("Producer", remote!!.optJSONObject("application")?.optString("versionName").orEmpty())
                Text(if (ready) "Comparison uses the complete live and remote technicalReport trees, including explicit unavailable and error states." else "No comparable complete live report is available. This does not imply unsupported capabilities.", color = Muted, fontSize = 13.sp)
            }
        }
        if (remote != null && ready) item {
            ScopeCard("Local / Database comparison", R.drawable.ic_compare) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Differences only", color = Foreground, modifier = Modifier.weight(1f))
                    Switch(differencesOnly, onCheckedChange = { differencesOnly = it; diffVisible = 100 })
                }
                OutlinedTextField(differenceSearch, onValueChange = { differenceSearch = it; diffVisible = 100 },
                    label = { Text("Search exact query paths & values") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                InfoLine("Matching leaves", visibleDiff.size.toString())
                InfoLine("Available comparison leaves", differences.size.toString())
            }
        }
        if (remote != null && ready) {
            items(visibleDiff.take(diffVisible), key = { it.path }) { entry ->
                ScopeCard(entry.path, R.drawable.ic_properties) {
                    InfoLine("Local", entry.local ?: "UNKNOWN / not reported")
                    InfoLine("Database", entry.remote ?: "UNKNOWN / not reported")
                    if (entry.local != entry.remote) StateLabel("DIFFERENT", Lime)
                }
            }
            if (visibleDiff.size > diffVisible) item {
                OutlinedButton(onClick = { diffVisible += 100 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Show next 100 of ${visibleDiff.size} technical leaves")
                }
            }
        }
    }
}
