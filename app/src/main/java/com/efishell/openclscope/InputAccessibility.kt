package com.efishell.openclscope

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
internal fun rememberPointerWheelScalePx(): Float {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) { android.view.ViewConfiguration.get(context).scaledVerticalScrollFactor.coerceAtLeast(1f) }
}

@Composable
internal fun Modifier.desktopVerticalPointerScroll(state: ScrollableState): Modifier =
    desktopVerticalPointerScroll(state, rememberPointerWheelScalePx())

internal fun Modifier.desktopVerticalPointerScroll(state: ScrollableState, wheelScalePx: Float): Modifier =
    this
        .pointerInput(state, wheelScalePx) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type != PointerEventType.Scroll) continue
                    val change = event.changes.firstOrNull { !it.isConsumed } ?: continue
                    val axis = if (change.scrollDelta.y != 0f) change.scrollDelta.y else change.scrollDelta.x
                    if (axis == 0f) continue
                    val consumed = state.dispatchRawDelta(axis * wheelScalePx)
                    if (consumed != 0f) change.consume()
                }
            }
        }
        .pointerInput(state) {
            val slop = viewConfiguration.touchSlop
            awaitPointerEventScope {
                var activeId: androidx.compose.ui.input.pointer.PointerId? = null
                var lastY = 0f
                var accumulatedY = 0f
                var dragging = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val mouse = event.changes.firstOrNull { change ->
                        change.type == PointerType.Mouse && (activeId == null || change.id == activeId)
                    }
                    if (mouse == null) {
                        if (!event.buttons.isPrimaryPressed) {
                            activeId = null
                            dragging = false
                            accumulatedY = 0f
                        }
                        continue
                    }
                    if (!event.buttons.isPrimaryPressed || !mouse.pressed) {
                        activeId = null
                        dragging = false
                        accumulatedY = 0f
                        continue
                    }
                    if (activeId == null) {
                        activeId = mouse.id
                        lastY = mouse.position.y
                        accumulatedY = 0f
                        dragging = false
                        continue
                    }
                    val deltaY = mouse.position.y - lastY
                    lastY = mouse.position.y
                    if (deltaY == 0f) continue
                    if (!dragging) {
                        accumulatedY += deltaY
                        if (kotlin.math.abs(accumulatedY) <= slop) continue
                        dragging = true
                        val slopDirection = if (accumulatedY > 0f) slop else -slop
                        val overSlop = accumulatedY - slopDirection
                        if (overSlop != 0f) state.dispatchRawDelta(-overSlop)
                        mouse.consume()
                    } else {
                        state.dispatchRawDelta(-deltaY)
                        mouse.consume()
                    }
                }
            }
        }

@Composable
internal fun Modifier.hardwareLazyDpad(state: LazyListState): Modifier {
    val focus = LocalFocusManager.current
    val coroutine = rememberCoroutineScope()
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val lateral = when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> FocusDirection.Left
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> FocusDirection.Right
            else -> null
        }
        if (lateral != null) return@onPreviewKeyEvent focus.moveFocus(lateral)
        val direction = when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> FocusDirection.Down
            AndroidKeyEvent.KEYCODE_DPAD_UP -> FocusDirection.Up
            else -> null
        }
        if (direction != null) {
            if (focus.moveFocus(direction)) return@onPreviewKeyEvent true
            val visible = state.layoutInfo
            if (visible.totalItemsCount <= 0) return@onPreviewKeyEvent false
            val target = if (direction == FocusDirection.Down) {
                ((visible.visibleItemsInfo.lastOrNull()?.index ?: state.firstVisibleItemIndex) + 1).coerceAtMost(visible.totalItemsCount - 1)
            } else {
                ((visible.visibleItemsInfo.firstOrNull()?.index ?: state.firstVisibleItemIndex) - 1).coerceAtLeast(0)
            }
            if (if (direction == FocusDirection.Down) !state.canScrollForward else !state.canScrollBackward) return@onPreviewKeyEvent false
            coroutine.launch { state.animateScrollToItem(target); delay(24L); focus.moveFocus(direction) }
            true
        } else when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_PAGE_DOWN -> if (!state.canScrollForward) false else {
                val index = (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: state.firstVisibleItemIndex).coerceAtMost((state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                coroutine.launch { state.animateScrollToItem(index) }
                true
            }
            AndroidKeyEvent.KEYCODE_PAGE_UP -> if (!state.canScrollBackward) false else {
                val index = (state.firstVisibleItemIndex - (state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) - 1)).coerceAtLeast(0)
                coroutine.launch { state.animateScrollToItem(index) }
                true
            }
            else -> false
        }
    }
}

@Composable
internal fun Modifier.hardwareLazyGridDpad(state: LazyGridState): Modifier {
    val focus = LocalFocusManager.current
    val coroutine = rememberCoroutineScope()
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val lateral = when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> FocusDirection.Left
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> FocusDirection.Right
            else -> null
        }
        if (lateral != null) return@onPreviewKeyEvent focus.moveFocus(lateral)
        val direction = when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> FocusDirection.Down
            AndroidKeyEvent.KEYCODE_DPAD_UP -> FocusDirection.Up
            else -> null
        }
        if (direction != null) {
            if (focus.moveFocus(direction)) return@onPreviewKeyEvent true
            val info = state.layoutInfo
            if (info.totalItemsCount <= 0) return@onPreviewKeyEvent false
            val target = if (direction == FocusDirection.Down) {
                ((info.visibleItemsInfo.maxOfOrNull { it.index } ?: state.firstVisibleItemIndex) + 1).coerceAtMost(info.totalItemsCount - 1)
            } else {
                ((info.visibleItemsInfo.minOfOrNull { it.index } ?: state.firstVisibleItemIndex) - 1).coerceAtLeast(0)
            }
            if (if (direction == FocusDirection.Down) !state.canScrollForward else !state.canScrollBackward) return@onPreviewKeyEvent false
            coroutine.launch { state.animateScrollToItem(target); delay(24L); focus.moveFocus(direction) }
            true
        } else when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_PAGE_DOWN -> if (!state.canScrollForward) false else {
                val target = (state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: state.firstVisibleItemIndex).coerceAtMost((state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                coroutine.launch { state.animateScrollToItem(target) }
                true
            }
            AndroidKeyEvent.KEYCODE_PAGE_UP -> if (!state.canScrollBackward) false else {
                val target = (state.firstVisibleItemIndex - state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)).coerceAtLeast(0)
                coroutine.launch { state.animateScrollToItem(target) }
                true
            }
            else -> false
        }
    }
}

@Composable
internal fun Modifier.hardwareScrollDpad(state: ScrollState): Modifier {
    val focus = LocalFocusManager.current
    val coroutine = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val lineStep = with(density) { 176.dp.roundToPx() }
    val pageStep = with(density) { (configuration.screenHeightDp.dp * 0.68f).roundToPx() }
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val lateral = when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> FocusDirection.Left
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> FocusDirection.Right
            else -> null
        }
        if (lateral != null) return@onPreviewKeyEvent focus.moveFocus(lateral)
        val direction = when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> FocusDirection.Down
            AndroidKeyEvent.KEYCODE_DPAD_UP -> FocusDirection.Up
            else -> null
        }
        if (direction != null) {
            if (focus.moveFocus(direction)) return@onPreviewKeyEvent true
            val dest = (state.value + if (direction == FocusDirection.Down) lineStep else -lineStep).coerceIn(0, state.maxValue)
            if (dest == state.value) false else {
                coroutine.launch { state.animateScrollTo(dest); delay(24L); focus.moveFocus(direction) }
                true
            }
        } else when (event.key.nativeKeyCode) {
            AndroidKeyEvent.KEYCODE_PAGE_DOWN -> {
                val dest = (state.value + pageStep).coerceAtMost(state.maxValue)
                if (dest == state.value) false else { coroutine.launch { state.animateScrollTo(dest) }; true }
            }
            AndroidKeyEvent.KEYCODE_PAGE_UP -> {
                val dest = (state.value - pageStep).coerceAtLeast(0)
                if (dest == state.value) false else { coroutine.launch { state.animateScrollTo(dest) }; true }
            }
            else -> false
        }
    }
}
