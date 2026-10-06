package com.efishell.openclscope

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(20.dp), pressedShape = RoundedCornerShape(26.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Foreground,
            disabledContainerColor = OpenCLSurfaceLow,
            disabledContentColor = Muted
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(18.dp), pressedShape = RoundedCornerShape(24.dp)),
        border = BorderStroke(1.dp, if (enabled) AccentSoft.copy(alpha = 0.46f) else OpenCLOutlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentSoft, disabledContentColor = Muted),
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    MaterialTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(18.dp), pressedShape = RoundedCornerShape(24.dp)),
        colors = ButtonDefaults.textButtonColors(contentColor = AccentSoft, disabledContentColor = Muted),
        content = content
    )
}
