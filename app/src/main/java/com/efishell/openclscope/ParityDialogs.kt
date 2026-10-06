package com.efishell.openclscope

import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null
) {
    MaterialAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.border(1.dp, AccentSoft.copy(alpha = 0.46f), MaterialTheme.shapes.extraLarge),
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = OpenCLSurfaceRaised,
        titleContentColor = Foreground,
        textContentColor = Foreground,
        tonalElevation = 0.dp
    )
}
