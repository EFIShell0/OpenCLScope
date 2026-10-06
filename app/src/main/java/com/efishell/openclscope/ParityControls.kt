package com.efishell.openclscope

import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch as MaterialSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
internal fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    MaterialSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = if (checked) {
            { Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Accent, modifier = Modifier.size(14.dp)) }
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Foreground,
            checkedTrackColor = Accent,
            uncheckedThumbColor = OpenCLTextSecondary,
            uncheckedTrackColor = OpenCLSurfaceTonal,
            uncheckedBorderColor = androidx.compose.ui.graphics.Color(0xFF494244)
        )
    )
}
