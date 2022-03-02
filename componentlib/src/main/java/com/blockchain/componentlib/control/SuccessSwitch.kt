package com.blockchain.componentlib.control

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Dark700
import com.blockchain.componentlib.theme.Green400
import com.blockchain.componentlib.theme.Green600
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey300

@Composable
fun SuccessSwitch(
    isChecked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    val checkedThumbColor = if (isDarkMode) Green400 else Green600
    val checkedTrackColor = if (isDarkMode) Green600 else Green400
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
            disabledCheckedTrackColor = uncheckedTrackColor,
            disabledUncheckedThumbColor = uncheckedThumbColor,
            disabledUncheckedTrackColor = uncheckedTrackColor,
        ),
        enabled = enabled,
        modifier = modifier
            .padding(dimensionResource(R.dimen.very_small_margin)),
    )
}

@Preview(name = "Not checked", group = "Success Switch")
@Composable
private fun SuccessSwitchPreview_NotChecked() {
    AppTheme {
        AppSurface {
            SuccessSwitch(
                isChecked = false,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Checked", group = "Success Switch")
@Composable
private fun SuccessSwitchPreview_IsChecked() {
    AppTheme {
        AppSurface {
            SuccessSwitch(
                isChecked = true,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Not checked not enabled", group = "Success Switch")
@Composable
private fun SuccessSwitchPreview_NotChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            SuccessSwitch(
                isChecked = false,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Checked not enabled", group = "Success Switch")
@Composable
private fun SuccessSwitchPreview_IsChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            SuccessSwitch(
                isChecked = true,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}
