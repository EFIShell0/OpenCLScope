package com.efishell.openclscope

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ParityChevronAffordance(label: String, contentDescription: String, onClick: () -> Unit) {
    Surface(color = AccentContainer, shape = RoundedCornerShape(18.dp), modifier = Modifier.semantics { role = Role.Button }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(start = 12.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)) {
            Text(label, color = AccentSoft, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = contentDescription, tint = AccentSoft, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun ParityInfoPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = OpenCLSurfaceTonal, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = Color(0xFF968D8F), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color(0xFFE7DFE1), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun ParityIdentityBlock(title: String, subtitle: String, icon: Int) {
    Surface(color = OpenCLSurfaceRaised, shape = MaterialTheme.shapes.largeIncreased, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF19351C)) {
                Icon(painterResource(icon), contentDescription = null, tint = AccentSoft, modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Foreground)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OpenCLTextSecondary)
            }
        }
    }
}

@Composable
internal fun ParityVersionBlock(application: String, version: String, versionCode: String, packageName: String, abi: String) {
    val expanded = openCLPreferExpandedTextLayoutForInfo()
    Surface(color = OpenCLSurfaceRaised, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Surface(shape = RoundedCornerShape(19.dp), color = Color(0xFF19351C)) {
                    Image(painterResource(R.drawable.openclscope_logo_foreground), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(46.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(application, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Version $version", color = AccentSoft, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF272224)) {
                    Text("#$versionCode", color = Color(0xFFC7BEC0), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
                }
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParityInfoPill("Installed ABI", abi, Modifier.fillMaxWidth())
                    ParityInfoPill("Package", packageName, Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParityInfoPill("Installed ABI", abi, Modifier.weight(1f))
                    ParityInfoPill("Package", packageName, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun openCLPreferExpandedTextLayoutForInfo(): Boolean {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.fontScale >= 1.3f || configuration.screenWidthDp < 360
}

@Composable
internal fun ParityActionButton(
    title: String,
    subtitle: String,
    icon: Int,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    trailingIcon: Int = R.drawable.ic_chevron_right,
    trailingTint: Color? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.largeIncreased
    val container = if (!enabled) Color(0xFF111111) else if (focused) Color(0xFF152A17) else Color(0xFF1A1718)
    val iconContainer = if (enabled) Color(0xFF19351C) else Color(0xFF181818)
    val accent = if (enabled) AccentSoft else Color(0xFF606064)
    val titleColor = if (enabled) Foreground else Color(0xFF6C696A)
    val detailColor = if (enabled) OpenCLTextSecondary else Color(0xFF5A5758)
    Surface(color = container, shape = shape, modifier = modifier.border(if (focused && enabled) 2.dp else 0.dp, if (focused && enabled) AccentSoft else Color.Transparent, shape)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = iconContainer) {
                Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = titleColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = detailColor, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp).onFocusChanged { focused = it.isFocused }, colors = IconButtonDefaults.iconButtonColors(containerColor = if (enabled) Color(0xFF172A19) else Color(0xFF171717), contentColor = accent, disabledContentColor = accent)) {
                AnimatedContent(targetState = trailingIcon, label = "actionTrailingIcon") { currentIcon ->
                    Icon(painterResource(currentIcon), contentDescription = title, tint = trailingTint ?: accent, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}


@Composable
internal fun ParityExternalLinkRow(title: String, subtitle: String, icon: Int, enabled: Boolean = true, onOpen: () -> Unit) {
    Surface(color = if (enabled) Color(0xFF1A1718) else Color(0xFF111111), shape = MaterialTheme.shapes.largeIncreased, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = if (enabled) Color(0xFF19351C) else Color(0xFF181818)) {
                Icon(painterResource(icon), contentDescription = null, tint = if (enabled) AccentSoft else Muted, modifier = Modifier.padding(11.dp).size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = if (enabled) Foreground else Muted, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = if (enabled) OpenCLTextSecondary else Muted, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onOpen, enabled = enabled, colors = IconButtonDefaults.iconButtonColors(containerColor = if (enabled) AccentContainer else OpenCLSurfaceLow, contentColor = AccentSoft, disabledContentColor = Muted), modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.ic_open_external), contentDescription = "Open external link", modifier = Modifier.size(20.dp))
            }
        }
    }
}
