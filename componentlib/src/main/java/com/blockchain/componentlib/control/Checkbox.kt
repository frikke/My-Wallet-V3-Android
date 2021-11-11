package com.blockchain.componentlib.control

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark900

@Composable
fun Checkbox(
    isChecked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
) {
    androidx.compose.material.Checkbox(
        checked = isChecked,
        onCheckedChange = onCheckChanged,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = AppTheme.colors.primary,
            uncheckedColor = AppTheme.colors.medium,
            checkmarkColor = if (isDarkTheme) Dark900 else Color.White,
            disabledColor = AppTheme.colors.medium.copy(alpha = ContentAlpha.disabled),
            disabledIndeterminateColor = AppTheme.colors.medium.copy(alpha = ContentAlpha.disabled),
        ),
    )
}

@Preview(name = "Not checked", group = "Checkbox")
@Composable
private fun CheckboxPreview_NotChecked() {
    AppTheme {
        AppSurface {
            Checkbox(
                isChecked = false,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Checked", group = "Checkbox")
@Composable
private fun CheckboxPreview_IsChecked() {
    AppTheme {
        AppSurface {
            Checkbox(
                isChecked = true,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Not checked not enabled", group = "Checkbox")
@Composable
private fun CheckboxPreview_NotChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            Checkbox(
                isChecked = false,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Checked not enabled", group = "Checkbox")
@Composable
private fun CheckboxPreview_IsChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            Checkbox(
                isChecked = true,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}
