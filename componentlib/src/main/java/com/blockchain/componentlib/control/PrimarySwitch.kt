package com.blockchain.componentlib.control

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

private val checkedThumbColorLight = Color(0XFF0C6CF2)
private val checkedThumbColorDark = Color(0XFF65A5FF)
private val checkedThumbColor
    @Composable get() = if (isSystemInDarkTheme()) checkedThumbColorDark else checkedThumbColorLight

private val checkedTrackColorLight = Color(0XFF65A5FF)
private val checkedTrackColorDark = Color(0XFF0C6CF2)
private val checkedTrackColor
    @Composable get() = if (isSystemInDarkTheme()) checkedTrackColorDark else checkedTrackColorLight

private val uncheckedThumbColorLight = Color(0XFFB1B8C7)
private val uncheckedThumbColorDark = Color(0XFF2C3038)
private val uncheckedThumbColor
    @Composable get() = if (isSystemInDarkTheme()) uncheckedThumbColorDark else uncheckedThumbColorLight

private val uncheckedTrackColorLight = Color(0XFFF1F2F7)
private val uncheckedTrackColorDark = Color(0XFF3B3E46)
private val uncheckedTrackColor
    @Composable get() = if (isSystemInDarkTheme()) uncheckedTrackColorDark else uncheckedTrackColorLight

@Composable
fun PrimarySwitch(
    isChecked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
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
            disabledUncheckedTrackColor = uncheckedTrackColor.copy(alpha = ContentAlpha.disabled)
        ),
        enabled = enabled,
        modifier = modifier
    )
}

@Preview(name = "Not checked", group = "Primary Switch")
@Composable
private fun PrimarySwitchPreview_NotChecked() {
    PrimarySwitch(
        isChecked = false,
        onCheckChanged = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimarySwitchPreview_NotCheckedDark() {
    PrimarySwitchPreview_NotChecked()
}

@Preview(name = "Checked", group = "Primary Switch")
@Composable
private fun PrimarySwitchPreview_IsChecked() {
    PrimarySwitch(
        isChecked = true,
        onCheckChanged = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimarySwitchPreview_IsCheckedDark() {
    PrimarySwitchPreview_IsChecked()
}

@Preview(name = "Not checked not enabled", group = "Primary Switch")
@Composable
private fun PrimarySwitchPreview_NotChecked_NotEnabled() {
    PrimarySwitch(
        isChecked = false,
        onCheckChanged = {},
        enabled = false
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimarySwitchPreview_NotChecked_NotEnabledDark() {
    PrimarySwitchPreview_NotChecked_NotEnabled()
}

@Preview(name = "Checked not enabled", group = "Primary Switch")
@Composable
private fun PrimarySwitchPreview_IsChecked_NotEnabled() {
    PrimarySwitch(
        isChecked = true,
        onCheckChanged = {},
        enabled = false
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimarySwitchPreview_IsChecked_NotEnabledDark() {
    PrimarySwitchPreview_IsChecked_NotEnabled()
}
