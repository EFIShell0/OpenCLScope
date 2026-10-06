package com.efishell.openclscope

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

@Composable
internal fun parityPageContentPadding(): PaddingValues {
    val navigationPadding = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val startInset = navigationPadding.calculateLeftPadding(layoutDirection)
    val endInset = navigationPadding.calculateRightPadding(layoutDirection)
    val transientOverlayInset = LocalTransientOverlayContentInset.current
    val coordinatedTransientOverlayInset by animateDpAsState(
        targetValue = transientOverlayInset,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "coordinatedTopOverlayInset"
    )
    return PaddingValues(
        start = 18.dp + startInset,
        top = LocalAppHeaderContentInset.current + PageChromeSeparation + coordinatedTransientOverlayInset,
        end = 18.dp + endInset,
        bottom = LocalBottomNavigationContentInset.current
    )
}

@Composable
internal fun parityScrollHintPadding(): PaddingValues {
    val navigationPadding = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val startInset = navigationPadding.calculateLeftPadding(layoutDirection)
    val endInset = navigationPadding.calculateRightPadding(layoutDirection)
    val transientOverlayInset = LocalTransientOverlayContentInset.current
    val scrollHintTopInset by animateDpAsState(
        targetValue = LocalAppHeaderContentInset.current + transientOverlayInset + 10.dp,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "scrollHintTopInset"
    )
    return PaddingValues(
        start = 12.dp + startInset,
        top = scrollHintTopInset,
        end = 12.dp + endInset,
        bottom = LocalBottomNavigationContentInset.current + 4.dp
    )
}
