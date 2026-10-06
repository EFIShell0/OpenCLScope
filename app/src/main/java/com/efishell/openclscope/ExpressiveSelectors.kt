package com.efishell.openclscope

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun OpenCLExpressiveFilterBar(labels: List<String>, selectedIndex: Int, title: String = "Filter", onSelected: (Int) -> Unit) {
    var expanded by rememberSaveable(labels) { mutableStateOf(false) }
    var mounted by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var query by rememberSaveable(labels) { mutableStateOf("") }
    var page by rememberSaveable(labels) { mutableIntStateOf(0) }
    val pageSize = 25
    val indexed = remember(labels, query) { labels.mapIndexed { i, v -> i to v }.filter { query.isBlank() || it.second.contains(query, true) } }
    val pageCount = maxOf(1, (indexed.size + pageSize - 1) / pageSize)
    val selectedLabel = labels.getOrNull(selectedIndex) ?: "Select"
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, tween(180), label = "openclFilterArrow")
    LaunchedEffect(expanded) {
        if (expanded) {
            query = ""
            page = (selectedIndex / pageSize).coerceIn(0, pageCount - 1)
            mounted = true
            visible = false
            delay(20)
            visible = true
        } else {
            visible = false
            delay(190)
            mounted = false
        }
    }
    LaunchedEffect(query, pageCount) { page = page.coerceIn(0, pageCount - 1) }
    BackHandler(enabled = expanded) { expanded = false }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val selectorShape = MaterialTheme.shapes.medium
        Surface(shape = selectorShape, color = OpenCLSurfaceTonal, contentColor = Foreground, border = BorderStroke(1.dp, Color(0xFF494244)), modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, color = Muted, style = MaterialTheme.typography.labelSmall)
                    Text(selectedLabel, color = Foreground, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                val arrowShape = RoundedCornerShape(15.dp)
                Surface(shape = arrowShape, color = if (expanded) Accent else AccentContainer, modifier = Modifier.size(44.dp).clip(arrowShape).clickable(role = Role.Button) { expanded = !expanded }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_expand_more), if (expanded) "Close filter menu" else "Open filter menu", tint = Foreground, modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = arrowRotation))
                    }
                }
            }
        }
        if (mounted) AnimatedVisibility(visible = visible, enter = fadeIn(tween(220)) + expandVertically(tween(220), expandFrom = Alignment.Top), exit = fadeOut(tween(150)) + shrinkVertically(tween(170), shrinkTowards = Alignment.Top)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = OpenCLSurfaceRaised, contentColor = Foreground, border = BorderStroke(1.dp, Color(0xFF494244)), shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (labels.size >= 5) {
                        OutlinedTextField(query, { query = it.take(120); page = 0 }, label = { Text("Search filters") }, placeholder = { Text("Type a filter name…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (indexed.isEmpty()) "No matching filters" else "${indexed.size} results · up to $pageSize per page", color = OpenCLTextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            if (pageCount > 1) Text("Page ${page + 1} / $pageCount", color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    val rows = indexed.drop(page * pageSize).take(pageSize)
                    val listState = rememberLazyListState()
                    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 410.dp).desktopVerticalPointerScroll(listState).hardwareLazyDpad(listState).focusGroup(), verticalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                        items(rows, key = { it.first }) { (index, label) ->
                            val selected = index == selectedIndex
                            val rowShape = RoundedCornerShape(18.dp)
                            Surface(shape = rowShape, color = if (selected) AccentContainer else Color.Transparent, modifier = Modifier.fillMaxWidth().clip(rowShape).clickable(role = Role.RadioButton) { onSelected(index); expanded = false }.parityTvBrowseModifier(rowShape)) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(shape = RoundedCornerShape(999.dp), color = if (selected) AccentSoft else OpenCLOutlineVariant, modifier = Modifier.size(10.dp)) {}
                                    Text(label, modifier = Modifier.weight(1f), color = if (selected) Foreground else OpenCLTextSecondary, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (rows.isEmpty()) item { Text("No filter matches this search.", color = Muted, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center) }
                    }
                    if (pageCount > 1 && indexed.isNotEmpty()) {
                        HorizontalDivider(color = OpenCLOutlineVariant)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IconButton(onClick = { if (page > 0) page-- }, enabled = page > 0, colors = IconButtonDefaults.iconButtonColors(containerColor = AccentContainer, contentColor = Foreground, disabledContainerColor = OpenCLSurfaceLow, disabledContentColor = Muted)) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous filter page") }
                                AnimatedContent(targetState = page + 1, transitionSpec = { fadeIn(tween(150)) + slideInHorizontally(tween(170)) { it / 6 } togetherWith fadeOut(tween(120)) + slideOutHorizontally(tween(140)) { -it / 6 } }, label = "openclFilterPage") { value -> Text("$value / $pageCount", color = Foreground, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center) }
                                IconButton(onClick = { if (page + 1 < pageCount) page++ }, enabled = page + 1 < pageCount, colors = IconButtonDefaults.iconButtonColors(containerColor = AccentContainer, contentColor = Foreground, disabledContainerColor = OpenCLSurfaceLow, disabledContentColor = Muted)) { Icon(painterResource(R.drawable.ic_chevron_right), "Next filter page") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OpenCLExpressiveFilterBar(labels: List<String>, selected: String, title: String = "Filter", onSelected: (String) -> Unit) {
    val index = labels.indexOf(selected).takeIf { it >= 0 } ?: 0
    OpenCLExpressiveFilterBar(labels, index, title) { onSelected(labels[it]) }
}

@Composable
internal fun OpenCLCollectionPager(totalItems: Int, currentPage: Int, onPageChange: (Int) -> Unit, pageSize: Int = 25) {
    val pageCount = maxOf(1, (totalItems + pageSize - 1) / pageSize)
    if (pageCount <= 1) return
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { if (currentPage > 0) onPageChange(currentPage - 1) }, enabled = currentPage > 0, colors = IconButtonDefaults.iconButtonColors(containerColor = AccentContainer, contentColor = Foreground, disabledContainerColor = OpenCLSurfaceLow, disabledContentColor = Muted)) {
                Icon(painterResource(R.drawable.ic_chevron_left), "Previous page")
            }
            Surface(shape = RoundedCornerShape(14.dp), color = OpenCLSurfaceTonal, border = BorderStroke(1.dp, OpenCLOutlineVariant)) {
                AnimatedContent(targetState = currentPage + 1, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(130)) }, label = "openclPageNumber") { page ->
                    Text("$page / $pageCount", modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Foreground, fontWeight = FontWeight.SemiBold)
                }
            }
            IconButton(onClick = { if (currentPage + 1 < pageCount) onPageChange(currentPage + 1) }, enabled = currentPage + 1 < pageCount, colors = IconButtonDefaults.iconButtonColors(containerColor = AccentContainer, contentColor = Foreground, disabledContainerColor = OpenCLSurfaceLow, disabledContentColor = Muted)) {
                Icon(painterResource(R.drawable.ic_chevron_right), "Next page")
            }
        }
    }
}
