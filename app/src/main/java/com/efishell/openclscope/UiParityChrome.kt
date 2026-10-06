package com.efishell.openclscope

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val PrimaryNavigationMaxWidth = 310.dp
internal val PrimaryNavigationHeight = 54.dp
internal val PrimaryNavigationIndicatorHeight = 42.dp
internal val PrimaryNavigationIndicatorHorizontalInset = 8.dp
internal val PrimaryNavigationBottomGap = 4.dp
internal val PrimaryNavigationContentGap = 10.dp
internal val OpenCLGlassTint = Color(0xA8171F18)
internal val OpenCLGlassBorder = Color(0x3D566A58)
internal val OpenCLTextSecondary = Color(0xFFB6ACAE)
internal val OpenCLOutlineVariant = Color(0xFF2A2527)
internal val OpenCLSurfaceLow = Color(0xFF111111)
internal val OpenCLSurfaceTonal = Color(0xFF211E1F)
internal val OpenCLPagerSurface = Color(0xFF1B1719)
internal val OpenCLSurfaceRaised = Color(0xFF181516)
internal val LocalTransientOverlayContentInset = staticCompositionLocalOf { 0.dp }
internal val LocalAppHeaderContentInset = staticCompositionLocalOf { 0.dp }
internal val PageChromeSeparation = 12.dp
internal val LocalValidatedInternetAvailable = staticCompositionLocalOf { false }
internal val LocalBottomNavigationContentInset = staticCompositionLocalOf { 74.dp }

internal data class ChromeBackdropSource(val layer: GraphicsLayer, val rootOffset: Offset)

@Composable
internal fun Modifier.parityTvBrowseModifier(shape: Shape): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) { if (focused) requester.bringIntoView() }
    return this.bringIntoViewRequester(requester).onFocusChanged { focused = it.isFocused }.focusable().border(if (focused) 2.dp else 0.dp, if (focused) AccentSoft else Color.Transparent, shape)
}

internal fun Modifier.frostedOpenCLChromeBackdrop(backdropLayer: GraphicsLayer, backdropRootOffset: Offset, targetRootOffset: Offset): Modifier = drawWithContent {
    clipRect {
        withTransform({ translate(backdropRootOffset.x - targetRootOffset.x, backdropRootOffset.y - targetRootOffset.y) }) {
            drawLayer(backdropLayer)
        }
        drawRect(OpenCLGlassTint)
    }
    drawContent()
}

@Composable
internal fun OpenCLSystemNavigationBackdrop(backdropLayer: GraphicsLayer, backdropRootOffset: Offset, modifier: Modifier = Modifier) {
    val navigationInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val bottomInset = navigationInsets.calculateBottomPadding()
    val leftInset = navigationInsets.calculateLeftPadding(layoutDirection)
    val rightInset = navigationInsets.calculateRightPadding(layoutDirection)
    Box(modifier) {
        if (bottomInset > 0.dp) {
            var targetRootOffset by remember { mutableStateOf(Offset.Zero) }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bottomInset).onGloballyPositioned { targetRootOffset = it.positionInRoot() }.frostedOpenCLChromeBackdrop(backdropLayer, backdropRootOffset, targetRootOffset))
        } else {
            if (leftInset > 0.dp) {
                var targetRootOffset by remember { mutableStateOf(Offset.Zero) }
                Box(Modifier.align(if (layoutDirection == LayoutDirection.Ltr) Alignment.CenterStart else Alignment.CenterEnd).fillMaxHeight().width(leftInset).onGloballyPositioned { targetRootOffset = it.positionInRoot() }.frostedOpenCLChromeBackdrop(backdropLayer, backdropRootOffset, targetRootOffset))
            }
            if (rightInset > 0.dp) {
                var targetRootOffset by remember { mutableStateOf(Offset.Zero) }
                Box(Modifier.align(if (layoutDirection == LayoutDirection.Ltr) Alignment.CenterEnd else Alignment.CenterStart).fillMaxHeight().width(rightInset).onGloballyPositioned { targetRootOffset = it.positionInRoot() }.frostedOpenCLChromeBackdrop(backdropLayer, backdropRootOffset, targetRootOffset))
            }
        }
    }
}

private fun selectedNavigationPage(page: Screen): Screen = when (page) {
    Screen.Overview -> Screen.Overview
    Screen.OpenCL -> Screen.OpenCL
    Screen.Devices -> Screen.Devices
    Screen.Extensions -> Screen.Extensions
    Screen.Features, Screen.Memory, Screen.Queues, Screen.Formats, Screen.Properties, Screen.Profiles, Screen.Analysis, Screen.Files, Screen.History, Screen.Watch, Screen.Minimum, Screen.Tests, Screen.Graph, Screen.Encyclopedia, Screen.Database, Screen.Settings, Screen.Info -> Screen.Overview
}

private val parityNavigationItems = listOf(Screen.Overview, Screen.OpenCL, Screen.Devices, Screen.Extensions)

@Composable
internal fun OpenCLCompactBottomNavigationBar(selectedPage: Screen, onPageSelected: (Screen) -> Unit, backdropLayer: GraphicsLayer, backdropRootOffset: Offset, modifier: Modifier = Modifier) {
    val navigationBarInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val bottomSystemInset = navigationBarInsets.calculateBottomPadding()
    val startSystemInset = navigationBarInsets.calculateLeftPadding(layoutDirection)
    val endSystemInset = navigationBarInsets.calculateRightPadding(layoutDirection)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactMaxWidth = if (landscape) 340.dp else PrimaryNavigationMaxWidth
    val compactHeight = if (landscape) 46.dp else PrimaryNavigationHeight
    val compactIndicatorHeight = if (landscape) 35.dp else PrimaryNavigationIndicatorHeight
    val compactIndicatorInset = if (landscape) 6.dp else PrimaryNavigationIndicatorHorizontalInset
    val compactIconSize = if (landscape) 18.dp else 20.dp
    val compactLabelFontSize = if (landscape) 8.sp else 9.sp
    val compactLabelLineHeight = if (landscape) 9.sp else 10.sp
    val compactItemTopPadding = if (landscape) 2.dp else 4.dp
    val compactItemBottomPadding = if (landscape) 2.dp else 3.dp
    val compactLabelSpacing = if (landscape) 1.dp else 2.dp
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp + startSystemInset, end = 16.dp + endSystemInset, bottom = PrimaryNavigationBottomGap + bottomSystemInset),
        contentAlignment = Alignment.BottomCenter
    ) {
        val targetWidth = maxWidth.coerceAtMost(compactMaxWidth)
        val navigationShape = RoundedCornerShape(999.dp)
        var navigationRootOffset by remember { mutableStateOf(Offset.Zero) }
        Surface(
            color = Color.Transparent,
            contentColor = Foreground,
            shape = navigationShape,
            border = BorderStroke(1.dp, OpenCLGlassBorder),
            shadowElevation = 10.dp,
            modifier = Modifier.width(targetWidth).height(compactHeight)
        ) {
            Box(Modifier.fillMaxSize().clip(navigationShape).onGloballyPositioned { navigationRootOffset = it.positionInRoot() }.frostedOpenCLChromeBackdrop(backdropLayer, backdropRootOffset, navigationRootOffset)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    parityNavigationItems.forEach { item ->
                        val selected = item == selectedNavigationPage(selectedPage)
                        val interactionSource = remember(item) { MutableInteractionSource() }
                        val indicatorAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(150, easing = FastOutSlowInEasing), label = "navigationIndicatorAlpha")
                        val indicatorScale by animateFloatAsState(if (selected) 1f else 0.86f, tween(180, easing = FastOutSlowInEasing), label = "navigationIndicatorScale")
                        val iconScale by animateFloatAsState(if (selected) 1f else 0.94f, tween(150, easing = FastOutSlowInEasing), label = "navigationIconScale")
                        val iconColor by animateColorAsState(if (selected) AccentSoft else Color(0xFFE3DEE0), tween(120), label = "navigationIconTint")
                        val textColor by animateColorAsState(if (selected) AccentSoft else Color(0xFFE9E3E6), tween(120), label = "navigationTextTint")
                        Box(
                            Modifier.weight(1f).fillMaxHeight().selectable(selected = selected, interactionSource = interactionSource, indication = null, role = Role.Tab, onClick = { onPageSelected(item) }).parityTvBrowseModifier(RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.fillMaxWidth().padding(horizontal = compactIndicatorInset).height(compactIndicatorHeight).graphicsLayer { alpha = indicatorAlpha; scaleX = indicatorScale; scaleY = 0.96f + indicatorAlpha * 0.04f }.clip(RoundedCornerShape(999.dp)).background(AccentContainer.copy(alpha = 0.82f)))
                            Column(Modifier.padding(top = compactItemTopPadding, bottom = compactItemBottomPadding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(painterResource(item.icon), contentDescription = null, tint = iconColor, modifier = Modifier.size(compactIconSize).graphicsLayer { scaleX = iconScale; scaleY = iconScale })
                                Spacer(Modifier.height(compactLabelSpacing))
                                Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontSize = compactLabelFontSize, lineHeight = compactLabelLineHeight, color = textColor, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChromeHeaderActionButton(icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, size: Dp = 50.dp) {
    val background = if (enabled) Color(0xFF19351C) else OpenCLSurfaceLow
    val content = if (enabled) AccentSoft else Muted
    Surface(
        shape = CircleShape,
        color = background,
        contentColor = content,
        border = BorderStroke(1.dp, if (enabled) AccentSoft.copy(alpha = 0.34f) else OpenCLOutlineVariant),
        tonalElevation = 2.dp,
        shadowElevation = 5.dp,
        modifier = modifier.size(size).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = contentDescription, tint = content, modifier = Modifier.size(if (size < 46.dp) 20.dp else 23.dp))
        }
    }
}

@Composable
private fun AnimatedChromeHeaderActionButton(visible: Boolean, icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, fromEnd: Boolean = false, buttonSize: Dp = 50.dp, slotSize: Dp = 50.dp) {
    Box(modifier.size(slotSize), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(190)) + slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { width -> if (fromEnd) width / 2 else -width / 2 } + scaleIn(animationSpec = tween(190), initialScale = 0.82f),
            exit = fadeOut(tween(150)) + slideOutHorizontally(tween(210, easing = FastOutSlowInEasing)) { width -> if (fromEnd) width / 2 else -width / 2 } + scaleOut(animationSpec = tween(150), targetScale = 0.82f)
        ) {
            ChromeHeaderActionButton(icon, contentDescription, onClick, enabled = enabled, size = buttonSize)
        }
    }
}

@Composable
private fun ChromeHeaderTitleBadge(title: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFFF5F0F1), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.width(28.dp).height(2.dp).clip(RoundedCornerShape(99.dp)).background(AccentSoft.copy(alpha = 0.88f)))
    }
}

@Composable
internal fun OpenCLParityAppHeader(page: Screen, onBack: () -> Unit, onSettings: () -> Unit, backdropLayer: GraphicsLayer, backdropRootOffset: Offset) {
    val showBack = page != Screen.Overview
    val showSettings = page != Screen.Settings
    val navigationPadding = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val navigationStartInset = navigationPadding.calculateLeftPadding(layoutDirection)
    val navigationEndInset = navigationPadding.calculateRightPadding(layoutDirection)
    var headerRootOffset by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxWidth().onGloballyPositioned { headerRootOffset = it.positionInRoot() }.frostedOpenCLChromeBackdrop(backdropLayer, backdropRootOffset, headerRootOffset)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 14.dp + navigationStartInset, end = 14.dp + navigationEndInset, top = 8.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedChromeHeaderActionButton(visible = showBack, icon = R.drawable.ic_back, contentDescription = "Back", onClick = onBack, buttonSize = 36.dp, slotSize = 50.dp)
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.foundation.Image(painterResource(R.drawable.openclscope_logo_horizontal_aligned), contentDescription = "OpenCLScope", contentScale = ContentScale.Fit, modifier = Modifier.width(126.dp).height(30.dp))
                Box(Modifier.width(1.dp).height(25.dp).background(Color.White.copy(alpha = 0.13f)))
                ChromeHeaderTitleBadge(page.label, Modifier.weight(1f))
            }
            AnimatedChromeHeaderActionButton(visible = showSettings, icon = R.drawable.ic_settings, contentDescription = "Settings", onClick = onSettings, fromEnd = true, buttonSize = 50.dp, slotSize = 50.dp)
        }
    }
}

internal enum class NetworkBannerState { HIDDEN, CONNECTED, DISCONNECTED }
internal data class OpenCLNetworkUiState(val known: Boolean, val available: Boolean, val transition: NetworkBannerState)

private fun validatedInternet(capabilities: NetworkCapabilities?): Boolean = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

@Composable
internal fun rememberOpenCLNetworkUiState(): OpenCLNetworkUiState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var known by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf(false) }
    var transition by remember { mutableStateOf(NetworkBannerState.HIDDEN) }
    var previous by remember { mutableStateOf<Boolean?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    val manager = remember(context) { context.getSystemService(ConnectivityManager::class.java) }
    fun apply(validated: Boolean) {
        val old = previous
        available = validated
        known = true
        if (old == null) {
            previous = validated
            transition = NetworkBannerState.HIDDEN
            return
        }
        if (old == validated) return
        previous = validated
        generation += 1
        transition = if (validated) NetworkBannerState.CONNECTED else NetworkBannerState.DISCONNECTED
    }
    LaunchedEffect(generation) {
        if (generation == 0) return@LaunchedEffect
        val localGeneration = generation
        delay(4_500L)
        if (generation == localGeneration) transition = NetworkBannerState.HIDDEN
    }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { scope.launch { apply(validatedInternet(manager.getNetworkCapabilities(network))) } }
            override fun onLost(network: Network) { scope.launch { apply(validatedInternet(manager.activeNetwork?.let(manager::getNetworkCapabilities))) } }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { scope.launch { apply(validatedInternet(capabilities)) } }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        scope.launch { apply(validatedInternet(manager.activeNetwork?.let(manager::getNetworkCapabilities))) }
        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
    return OpenCLNetworkUiState(known, available, transition)
}

internal enum class ChromeCollectionStatus { IDLE, COLLECTING, COMPLETED, FAILED }

@Composable
internal fun rememberChromeCollectionStatus(collecting: Boolean, report: CLSnapshot?): ChromeCollectionStatus {
    var status by remember { mutableStateOf(if (collecting) ChromeCollectionStatus.COLLECTING else ChromeCollectionStatus.IDLE) }
    var wasCollecting by remember { mutableStateOf(collecting) }
    LaunchedEffect(collecting, report?.status) {
        if (collecting) {
            wasCollecting = true
            status = ChromeCollectionStatus.COLLECTING
        } else if (wasCollecting) {
            wasCollecting = false
            status = if (report?.status == "ok") ChromeCollectionStatus.COMPLETED else ChromeCollectionStatus.FAILED
            delay(2_800L)
            status = ChromeCollectionStatus.IDLE
        }
    }
    return status
}

@Composable
private fun FloatingStatusSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().widthIn(max = 520.dp),
        color = Color(0xD61A1A1F),
        contentColor = Foreground,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0x423A3438)),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) { content() }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollectionStatusBanner(status: ChromeCollectionStatus) {
    AnimatedVisibility(
        visible = status != ChromeCollectionStatus.IDLE,
        enter = fadeIn(tween(260)) + expandVertically(tween(260)),
        exit = fadeOut(tween(420)) + shrinkVertically(tween(420))
    ) {
        val collecting = status == ChromeCollectionStatus.COLLECTING
        val failed = status == ChromeCollectionStatus.FAILED
        FloatingStatusSurface(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (collecting) {
                        Surface(shape = RoundedCornerShape(50), color = AccentContainer, modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_action_update), null, tint = AccentSoft, modifier = Modifier.size(20.dp)) }
                        }
                        Text("OpenCLScope is collecting OpenCL information in the background.", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    } else {
                        val stateColor = if (failed) Color(0xFFFF7676) else Color(0xFF55D98A)
                        val stateContainer = if (failed) Color(0xFF431C20) else Color(0xFF163D24)
                        Surface(shape = RoundedCornerShape(50), color = stateContainer, modifier = Modifier.size(30.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(painterResource(if (failed) R.drawable.ic_close else R.drawable.ic_check), null, tint = stateColor, modifier = Modifier.size(18.dp)) }
                        }
                        Text(if (failed) "Failed" else "Completed", color = stateColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (failed) "No complete OpenCL information was collected." else "OpenCL information updated.", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    }
                }
                if (collecting) LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(12.dp), color = AccentSoft, trackColor = Color(0xFF202A20))
            }
        }
    }
}

@Composable
private fun NetworkStatusBanner(state: NetworkBannerState) {
    var renderedState by remember { mutableStateOf(NetworkBannerState.CONNECTED) }
    LaunchedEffect(state) { if (state != NetworkBannerState.HIDDEN) renderedState = state }
    AnimatedVisibility(
        visible = state != NetworkBannerState.HIDDEN,
        enter = fadeIn(tween(240)) + expandVertically(tween(240)),
        exit = fadeOut(tween(360)) + shrinkVertically(tween(360))
    ) {
        val connected = renderedState == NetworkBannerState.CONNECTED
        val stateColor = if (connected) Color(0xFF55D98A) else Color(0xFFFF7676)
        val stateContainer = if (connected) Color(0xFF163D24) else Color(0xFF431C20)
        FloatingStatusSurface(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(50), color = stateContainer, modifier = Modifier.size(30.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(painterResource(if (connected) R.drawable.ic_network_connected else R.drawable.ic_network_disconnected), null, tint = stateColor, modifier = Modifier.size(17.dp)) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(if (connected) "Connected to network" else "No internet connection", color = stateColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(if (connected) "Android reports a validated default network." else "Android does not currently report a validated default network.", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun OfflineFeatureAvailabilityBanner(collectionInProgress: Boolean) {
    FloatingStatusSurface(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(50), color = Color(0xFF16344F), modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_info), null, tint = Color(0xFF5CA9FF), modifier = Modifier.size(17.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Internet features are unavailable", color = Color(0xFF9CCBFF), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(if (collectionInProgress) "OpenCL collection continues offline. Internet-dependent actions remain locked by network state, while report-dependent actions also remain locked until collection completes." else "OpenCL inspection stays available offline. Database submission/fetching, web links and update checks remain disabled until Android reports a validated internet connection.", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ConnectivityStatusHost(collectionStatus: ChromeCollectionStatus, network: OpenCLNetworkUiState) {
    var showPersistentOffline by remember { mutableStateOf(false) }
    LaunchedEffect(network.known, network.available, network.transition) {
        showPersistentOffline = false
        if (network.known && !network.available && network.transition == NetworkBannerState.HIDDEN) {
            delay(380L)
            showPersistentOffline = true
        }
    }
    NetworkStatusBanner(network.transition)
    AnimatedVisibility(
        visible = showPersistentOffline,
        enter = fadeIn(tween(240)) + expandVertically(tween(240)),
        exit = fadeOut(tween(220)) + shrinkVertically(tween(220))
    ) { OfflineFeatureAvailabilityBanner(collectionStatus == ChromeCollectionStatus.COLLECTING) }
}

internal sealed interface ChromeUpdateStatus {
    data object Hidden : ChromeUpdateStatus
    data object Checking : ChromeUpdateStatus
    data object UpToDate : ChromeUpdateStatus
    data class Available(val version: String) : ChromeUpdateStatus
    data class Failed(val message: String) : ChromeUpdateStatus
}

@Composable
internal fun rememberChromeUpdateStatus(checking: Boolean, release: GitHubRelease?, statusText: String): ChromeUpdateStatus {
    var rendered by remember { mutableStateOf<ChromeUpdateStatus>(ChromeUpdateStatus.Hidden) }
    var generation by remember { mutableIntStateOf(0) }
    LaunchedEffect(checking, release?.tag, statusText) {
        val next = when {
            checking -> ChromeUpdateStatus.Checking
            release != null && release.tag.removePrefix("v") != BuildConfig.VERSION_NAME -> ChromeUpdateStatus.Available(release.tag.removePrefix("v"))
            statusText == "OpenCLScope is up to date" -> ChromeUpdateStatus.UpToDate
            statusText.startsWith("Release check:") -> ChromeUpdateStatus.Failed(statusText.removePrefix("Release check:").trim())
            else -> ChromeUpdateStatus.Hidden
        }
        if (next != rendered) { rendered = next; generation += 1 }
    }
    LaunchedEffect(generation, rendered) {
        val local = generation
        val duration = when (rendered) {
            ChromeUpdateStatus.UpToDate -> 8_000L
            ChromeUpdateStatus.Hidden, ChromeUpdateStatus.Checking -> return@LaunchedEffect
            else -> 10_000L
        }
        delay(duration)
        if (generation == local) rendered = ChromeUpdateStatus.Hidden
    }
    return rendered
}

@Composable
private fun UpdateStatusBanner(status: ChromeUpdateStatus, onReview: () -> Unit) {
    AnimatedVisibility(
        visible = status !is ChromeUpdateStatus.Hidden,
        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
        exit = fadeOut(tween(360)) + shrinkVertically(tween(360))
    ) {
        FloatingStatusSurface(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when (status) {
                    ChromeUpdateStatus.Checking -> {
                        Surface(shape = RoundedCornerShape(50), color = AccentContainer, modifier = Modifier.size(30.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_check_updates), null, tint = AccentSoft, modifier = Modifier.size(17.dp)) } }
                        Text("Checking for updates…", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    }
                    ChromeUpdateStatus.UpToDate -> {
                        Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF163D24)) { Text("UP TO DATE", color = Color(0xFF55D98A), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)) }
                        Text("OpenCLScope is up to date.", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    }
                    is ChromeUpdateStatus.Available -> {
                        Surface(shape = RoundedCornerShape(12.dp), color = AccentContainer, modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_update_available), null, tint = AccentSoft, modifier = Modifier.size(20.dp)) } }
                        Text("OpenCLScope ${status.version} available", color = AccentSoft, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = onReview, modifier = Modifier.size(42.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = AccentContainer, contentColor = AccentSoft)) { Icon(painterResource(R.drawable.ic_chevron_right), "Review update", modifier = Modifier.size(18.dp)) }
                    }
                    is ChromeUpdateStatus.Failed -> Text(status.message, color = Color(0xFFFF8A8A), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    ChromeUpdateStatus.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
internal fun OpenCLTransientStatusOverlayHost(collectionStatus: ChromeCollectionStatus, network: OpenCLNetworkUiState, updateStatus: ChromeUpdateStatus, onReviewUpdate: () -> Unit, modifier: Modifier = Modifier) {
    val overlayVisible = collectionStatus != ChromeCollectionStatus.IDLE || network.transition != NetworkBannerState.HIDDEN || (network.known && !network.available) || updateStatus !is ChromeUpdateStatus.Hidden
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (overlayVisible) 8.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CollectionStatusBanner(collectionStatus)
        ConnectivityStatusHost(collectionStatus, network)
        UpdateStatusBanner(updateStatus, onReviewUpdate)
    }
}

@Composable
internal fun ParityDestinationCard(title: String, subtitle: String, icon: Int, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.large
    Surface(color = OpenCLSurfaceRaised, shape = shape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF19351C)) {
                Icon(painterResource(icon), null, tint = AccentSoft, modifier = Modifier.padding(10.dp).size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Foreground)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OpenCLTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onClick, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF192B1B), contentColor = AccentSoft)) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Open $title", tint = AccentSoft, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun ParitySectionButton(title: String, icon: Int, action: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.largeIncreased
    val container = if (focused) Color(0xFF152A17) else OpenCLSurfaceTonal
    Surface(
        color = container,
        shape = shape,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.border(if (focused) 2.dp else 0.dp, if (focused) AccentSoft else Color.Transparent, shape).clickable(role = Role.Button, onClick = action).focusable()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF19351C)) {
                Icon(painterResource(icon), null, tint = AccentSoft, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Text(title, color = Foreground, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = action, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF192B1B), contentColor = AccentSoft)) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = title, tint = AccentSoft, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
internal fun ParitySettingsSectionCards(onSelected: (String) -> Unit) {
    val sections = listOf(
        Triple("Info", "Developer, application, libraries, build, Android/device and OpenCL registry information.", R.drawable.ic_info),
        Triple("Reports & Database", "Complete TXT/HTML export, Database submission, compatibility state and public report access.", R.drawable.ic_database_submit),
        Triple("Runtime & Update Preferences", "OpenCL runtime information, built-in GitHub update preference and Obtainium guidance.", R.drawable.ic_settings)
    )
    var cardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardsVisible = true }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sections.forEachIndexed { index, section ->
            AnimatedVisibility(
                visible = cardsVisible,
                enter = fadeIn(tween(durationMillis = 260, delayMillis = index * 45)) + slideInHorizontally(tween(durationMillis = 260, delayMillis = index * 45)) { it / 10 },
                exit = fadeOut(tween(durationMillis = 120))
            ) { ParityDestinationCard(section.first, section.second, section.third) { onSelected(section.first) } }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ParityPrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    MaterialButton(onClick = onClick, enabled = enabled, shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(20.dp), pressedShape = RoundedCornerShape(26.dp)), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Foreground)) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ParityTextButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    MaterialTextButton(onClick = onClick, enabled = enabled, shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(18.dp), pressedShape = RoundedCornerShape(24.dp)), colors = ButtonDefaults.textButtonColors(contentColor = AccentSoft)) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}
