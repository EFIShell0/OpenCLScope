package com.efishell.openclscope

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

private fun openCLVendorLogo(vendor: String): Int {
    val value = vendor.lowercase(Locale.ROOT)
    return when {
        "qualcomm" in value -> R.drawable.gpu_vendor_qualcomm
        "arm" in value -> R.drawable.gpu_vendor_arm
        "imagination" in value || "powervr" in value -> R.drawable.gpu_vendor_imagination
        "huawei" in value || "hisilicon" in value -> R.drawable.gpu_vendor_huawei
        "nvidia" in value -> R.drawable.gpu_vendor_nvidia
        "advanced micro" in value || "amd" in value -> R.drawable.gpu_vendor_amd
        "intel" in value -> R.drawable.gpu_vendor_intel
        "samsung" in value -> R.drawable.gpu_vendor_samsung
        "broadcom" in value -> R.drawable.gpu_vendor_broadcom
        "vivante" in value -> R.drawable.gpu_vendor_vivante
        "verisilicon" in value -> R.drawable.gpu_vendor_vsi
        else -> R.drawable.gpu_vendor_unknown
    }
}

@Composable
private fun OpenCLVendorLogo(device: CLDevice?, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = OpenCLSurfaceLow), shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Image(
            painter = painterResource(openCLVendorLogo(device?.vendor.orEmpty())),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OpenCLHeroCard(report: CLSnapshot?, device: CLDevice?, origin: String, collecting: Boolean) {
    Surface(color = OpenCLSurfaceRaised, shape = MaterialTheme.shapes.extraLargeIncreased, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpenCLVendorLogo(device, Modifier.size(82.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        device?.name ?: when {
                            collecting -> "Collecting OpenCL capabilities"
                            report?.status == "unavailable" -> "OpenCL runtime unavailable"
                            report?.status == "error" -> "OpenCL inspection error"
                            else -> "OpenCL device unavailable"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val deviceEnumerationFailure = report?.platforms?.firstOrNull { it.deviceEnumerationStatus == "error" }
                    val deviceEnumerationEmpty = report?.platforms?.isNotEmpty() == true && report.platforms.all { it.deviceEnumerationStatus == "no_devices" }
                    Text(
                        device?.vendor ?: when {
                            deviceEnumerationFailure != null -> "clGetDeviceIDs · ${openCLErrorLabel(deviceEnumerationFailure.deviceEnumerationCode)}"
                            deviceEnumerationEmpty -> "clGetDeviceIDs · ${openCLErrorLabel(-1)}"
                            else -> report?.message.orEmpty().ifBlank { "Unknown vendor" }
                        },
                        color = OpenCLTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (origin == "Imported") "Imported report" else "System OpenCL runtime",
                        color = if (origin == "Imported") OpenCLTextSecondary else Foreground,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (device != null) {
                InfoLine("OpenCL version", device.version)
                Text(
                    "GPU name, vendor and OpenCL version are shown from values returned by the active OpenCL implementation. The vendor logo is only a presentation mapping of the runtime vendor string.",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall
                )
            } else if (report != null) {
                val enumerationFailure = report.platforms.firstOrNull { it.deviceEnumerationStatus == "error" }
                val noDevices = report.platforms.isNotEmpty() && report.platforms.all { it.deviceEnumerationStatus == "no_devices" }
                when {
                    enumerationFailure != null -> {
                        InfoLine("Device enumeration", openCLErrorLabel(enumerationFailure.deviceEnumerationCode), "error")
                        Text("The runtime was reached, but device enumeration returned an OpenCL error. This is reported as an enumeration error and is not converted to unsupported hardware.", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    noDevices -> {
                        InfoLine("Device enumeration", openCLErrorLabel(-1), "unavailable")
                        Text("The loader and at least one platform were reached, but no OpenCL device was exposed to this process. This is unavailable evidence, not proof that the GPU lacks OpenCL hardware support.", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    report.status != "ok" -> Text(
                        "A loader that is absent or inaccessible to this Android process is reported as unavailable, not as proof that the GPU lacks OpenCL hardware support.",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenCLMetricCard(title: String, value: String, modifier: Modifier) {
    val shape = MaterialTheme.shapes.large
    Surface(
        color = AccentContainer,
        contentColor = Foreground,
        shape = shape,
        border = BorderStroke(1.dp, AccentSoft.copy(alpha = 0.34f)),
        modifier = modifier.animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing)).parityTvBrowseModifier(shape)
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = AccentSoft, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(value, color = Foreground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OpenCLCapabilitySectionCard(title: String, icon: Int, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    Surface(color = OpenCLSurfaceRaised, shape = shape, modifier = Modifier.fillMaxWidth().parityTvBrowseModifier(shape)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF19351C)) {
                    OpenCLSectionHeaderIcon(title, openCLSectionIcon(title, icon))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Foreground, modifier = Modifier.weight(1f).semantics { heading() })
            }
            HorizontalDivider(color = OpenCLOutlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

@Composable
private fun OpenCLExploreDestinationTile(page: Screen, navigate: (Screen) -> Unit, modifier: Modifier) {
    val shape = MaterialTheme.shapes.medium
    Card(
        onClick = { navigate(page) },
        colors = CardDefaults.cardColors(containerColor = OpenCLSurfaceTonal),
        shape = shape,
        modifier = modifier.heightIn(min = 52.dp).parityTvBrowseModifier(shape)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.small, color = AccentContainer) {
                Icon(painterResource(page.icon), contentDescription = null, tint = AccentSoft, modifier = Modifier.padding(7.dp).size(18.dp))
            }
            Text(page.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun openCLPreferExpandedTextLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.fontScale >= 1.3f || configuration.screenWidthDp < 360
}

@Composable
private fun OpenCLExploreCard(navigate: (Screen) -> Unit) {
    val expandedTextLayout = openCLPreferExpandedTextLayout()
    val pages = listOf(Screen.Features, Screen.Memory, Screen.Queues, Screen.Profiles, Screen.Formats, Screen.Properties)
    OpenCLCapabilitySectionCard("Explore", R.drawable.ic_quick_access_grid) {
        Text("Detailed OpenCL inspection areas", color = Color(0xFF8F8F8F), style = MaterialTheme.typography.bodySmall)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = when {
                expandedTextLayout || maxWidth < 300.dp -> 1
                maxWidth < 620.dp -> 2
                else -> 3
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                pages.chunked(columns).forEach { rowPages ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        rowPages.forEach { page -> OpenCLExploreDestinationTile(page, navigate, Modifier.weight(1f)) }
                        repeat(columns - rowPages.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenCLQuickAccessCard(title: String, destination: Screen, navigate: (Screen) -> Unit, modifier: Modifier) {
    Card(
        onClick = { navigate(destination) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1718)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.heightIn(min = 72.dp).parityTvBrowseModifier(MaterialTheme.shapes.medium)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
            Icon(painterResource(destination.icon), contentDescription = null, modifier = Modifier.size(19.dp), tint = AccentSoft)
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OpenCLDestinationCard(title: String, subtitle: String, destination: Screen, navigate: (Screen) -> Unit) {
    val shape = MaterialTheme.shapes.large
    Surface(color = OpenCLSurfaceRaised, shape = shape, modifier = Modifier.fillMaxWidth().parityTvBrowseModifier(shape)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF19351C)) {
                Icon(painterResource(destination.icon), contentDescription = null, tint = AccentSoft, modifier = Modifier.padding(10.dp).size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Foreground)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OpenCLTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { navigate(destination) }, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF172A19), contentColor = AccentSoft)) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Open $title", tint = AccentSoft, modifier = Modifier.size(20.dp))
            }
        }
    }
}

internal fun LazyListScope.vulkanParityOpenCLOverview(report: CLSnapshot?, device: CLDevice?, origin: String, collecting: Boolean, navigate: (Screen) -> Unit) {
    item { OpenCLHeroCard(report, device, origin, collecting) }
    if (report != null && report.status != "ok") {
        item {
            OpenCLCapabilitySectionCard("OpenCL inspection error", R.drawable.ic_info) {
                Text(report.message.ifBlank { "The current process could not enumerate a usable OpenCL platform." }, color = Color(0xFFFF6B6B))
                Text("Runtime visibility is shown separately from GPU capability. Unavailable is not treated as unsupported.", color = Color(0xFF9E9E9E), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    item { OpenCLExploreCard(navigate) }
    val version = device?.version ?: "Unknown"
    val deviceType = device?.type ?: "Unknown"
    val computeUnits = device?.value("CL_DEVICE_MAX_COMPUTE_UNITS") ?: "Unknown"
    val memory = device?.value("CL_DEVICE_GLOBAL_MEM_SIZE")?.toLongOrNull()?.let(::formatOpenCLBytes) ?: "Unknown"
    item {
        if (openCLPreferExpandedTextLayout()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OpenCLMetricCard("OpenCL", version, Modifier.fillMaxWidth())
                OpenCLMetricCard("Device type", deviceType, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OpenCLMetricCard("OpenCL", version, Modifier.weight(1f))
                OpenCLMetricCard("Device type", deviceType, Modifier.weight(1f))
            }
        }
    }
    item {
        if (openCLPreferExpandedTextLayout()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OpenCLMetricCard("Compute units", computeUnits, Modifier.fillMaxWidth())
                OpenCLMetricCard("Global memory", memory, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OpenCLMetricCard("Compute units", computeUnits, Modifier.weight(1f))
                OpenCLMetricCard("Global memory", memory, Modifier.weight(1f))
            }
        }
    }
    item {
        OpenCLCapabilitySectionCard("Quick access", R.drawable.ic_quick_access_grid) {
            val quick = listOf(
                "OpenCL" to Screen.OpenCL,
                "Device" to Screen.Devices,
                "Images" to Screen.Formats,
                "Memory" to Screen.Memory,
                "Extensions" to Screen.Extensions,
                "Profiles" to Screen.Profiles,
                "Queues" to Screen.Queues,
                "More" to Screen.Features
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    openCLPreferExpandedTextLayout() || maxWidth < 300.dp -> 1
                    maxWidth < 540.dp -> 2
                    maxWidth < 780.dp -> 3
                    else -> 4
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quick.chunked(columns).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowItems.forEach { (title, destination) -> OpenCLQuickAccessCard(title, destination, navigate, Modifier.weight(1f)) }
                            repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
    item {
        OpenCLCapabilitySectionCard("Capability snapshot", R.drawable.ic_properties) {
            InfoLine("GPU", device?.name ?: "Unknown")
            InfoLine("Driver", device?.driverVersion ?: "Unknown")
            InfoLine("Vendor", device?.vendor ?: "Unknown")
            InfoLine("Device type", device?.type ?: "Unknown")
            InfoLine("Loader", report?.loader?.ifBlank { "Unavailable" } ?: "Unknown")
            InfoLine("Platforms", report?.platforms?.size?.toString() ?: "Unknown")
            InfoLine("Device extensions", device?.extensions?.size?.toString() ?: "Unknown")
            InfoLine("Device query results", device?.properties?.size?.toString() ?: "Unknown")
        }
    }
    item { OpenCLDestinationCard("Encyclopedia", "Search OpenCL terms, CL_* symbols, commands, types, extensions and error meanings.", Screen.Encyclopedia, navigate) }
    item { OpenCLDestinationCard("Analysis workspace", "Open local comparison, profile minimums, dependency graph, diagnostics, watched evidence, sharing and optional tests.", Screen.Analysis, navigate) }
}

private fun formatOpenCLBytes(value: Long): String {
    if (value <= 0L) return "0 B"
    val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
    var number = value.toDouble()
    var index = 0
    while (number >= 1024.0 && index < units.lastIndex) {
        number /= 1024.0
        index++
    }
    return "%.2f %s".format(Locale.US, number, units[index])
}
