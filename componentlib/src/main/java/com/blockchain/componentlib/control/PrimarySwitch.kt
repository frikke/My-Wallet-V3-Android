package com.blockchain.componentlib.control

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Dark700
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey300

@Composable
fun PrimarySwitch(
    isChecked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    val checkedThumbColor = if (isDarkMode) Blue400 else Blue600
    val checkedTrackColor = if (isDarkMode) Blue600 else Blue400
    val uncheckedThumbColor = if (isDarkMode) Dark700 else Grey300
    val uncheckedTrackColor = if (isDarkMode) Dark600 else Grey000

    Switch(
        checked = isChecked,
        onCheckedChange = { onCheckChanged(it) },
        colors = SwitchDefaults.colors(
            checkedThumbColor = checkedThumbColor,
            checkedTrackColor = checkedTrackColor,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            disabledCheckedThumbColor = uncheckedThumbColor,
            disabledCheckedTrackColor = uncheckedTrackColor.copy(alpha = ContentAlpha.disabled),
            disabledUncheckedThumbColor = uncheckedThumbColor,
            disabledUncheckedTrackColor = uncheckedTrackColor.copy(alpha = ContentAlpha.disabled),
        ),
        enabled = enabled,
        modifier = modifier
            .padding(12.dp),
    )
}

@Preview(name = "Not checked", group = "Primary Switch")
@Composable
private fun PrimarySwitchPreview_NotChecked() {
    AppTheme {
        AppSurface {
            PrimarySwitch(
                isChecked = false,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Checked", group = "Primary Switch")
@Composable
private fun PrimarySwitchPreview_IsChecked() {
    AppTheme {
        AppSurface {
            PrimarySwitch(
                isChecked = true,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Not checked not enabled", group = "Primary Switch")
@Composable
private fun PrimarySwitchPreview_NotChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            PrimarySwitch(
                isChecked = false,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Checked not enabled", group = "Primary Switch")
@Composable
private fun PrimarySwitchPreview_IsChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            PrimarySwitch(
                isChecked = true,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}
