package com.efishell.openclscope

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun OpenCLScopeOpeningAnimation(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    val opacity by animateFloatAsState(if (phase >= 4) 0f else if (phase >= 1) 1f else 0f, tween(if (phase >= 4) 260 else 360, easing = FastOutSlowInEasing), label = "opening-opacity")
    val logoScale by animateFloatAsState(when { phase >= 4 -> 1.025f; phase >= 2 -> 1f; phase >= 1 -> 1.012f; else -> .94f }, tween(if (phase >= 4) 280 else 520, easing = FastOutSlowInEasing), label = "opening-logo-scale")
    val haloAlpha by animateFloatAsState(when { phase >= 4 -> 0f; phase >= 3 -> .26f; phase >= 2 -> .52f; phase >= 1 -> .34f; else -> 0f }, tween(460, easing = FastOutSlowInEasing), label = "opening-halo-opacity")
    val haloScale by animateFloatAsState(when { phase >= 4 -> 1.08f; phase >= 2 -> 1f; phase >= 1 -> .92f; else -> .84f }, tween(620, easing = FastOutSlowInEasing), label = "opening-halo-scale")
    val lineScale by animateFloatAsState(if (phase >= 2) 1f else if (phase >= 1) .34f else 0f, tween(520, easing = FastOutSlowInEasing), label = "opening-line-scale")
    val lineAlpha by animateFloatAsState(if (phase >= 4) 0f else if (phase >= 1) 1f else 0f, tween(if (phase >= 4) 220 else 300, easing = FastOutSlowInEasing), label = "opening-line-alpha")
    val accentPulse by animateFloatAsState(when { phase >= 4 -> 0f; phase >= 3 -> .28f; phase >= 2 -> .82f; phase >= 1 -> .46f; else -> 0f }, tween(420, easing = FastOutSlowInEasing), label = "opening-accent-pulse")
    LaunchedEffect(Unit) {
        if (!ValueAnimator.areAnimatorsEnabled()) {
            onFinished()
            return@LaunchedEffect
        }
        phase = 1
        delay(520L)
        phase = 2
        delay(500L)
        phase = 3
        delay(300L)
        phase = 4
        delay(280L)
        onFinished()
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF070A07), Color(0xFF070D08), Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        val landscape = maxWidth > maxHeight
        val halo = if (landscape) 620.dp else 470.dp
        val core = if (landscape) 390.dp else 300.dp
        val line = if (landscape) 180.dp else 142.dp
        Box(Modifier.size(halo).graphicsLayer { alpha = haloAlpha; scaleX = haloScale; scaleY = haloScale }.background(
            Brush.radialGradient(listOf(Color(0x66315C22), Color(0x33132611), Color.Transparent), radius = if (landscape) 560f else 450f)
        ))
        Box(Modifier.size(core).graphicsLayer { alpha = accentPulse; scaleX = .9f + accentPulse * .12f; scaleY = .9f + accentPulse * .12f }.background(
            Brush.radialGradient(listOf(Color(0x473DAE2B), Color(0x24162813), Color.Transparent))
        ))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (landscape) 18.dp else 16.dp),
            modifier = Modifier.graphicsLayer { alpha = opacity; scaleX = logoScale; scaleY = logoScale }
        ) {
            Image(
                painterResource(R.drawable.openclscope_logo_horizontal_aligned),
                "OpenCLScope",
                modifier = Modifier.widthIn(max = if (landscape) 560.dp else 360.dp)
                    .fillMaxWidth(if (landscape) .52f else .78f)
                    .heightIn(max = if (landscape) 112.dp else 96.dp),
                contentScale = ContentScale.Fit
            )
            Box(Modifier.width(line + 20.dp).height(2.dp).graphicsLayer { alpha = lineAlpha; scaleX = lineScale }
                .clip(RoundedCornerShape(999.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF164A18), Color(0xFF3DAE2B), Color(0xFF164A18)))))
        }
    }
}
