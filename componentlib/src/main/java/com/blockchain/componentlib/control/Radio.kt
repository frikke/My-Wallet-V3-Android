package com.blockchain.componentlib.control

import androidx.compose.material.ContentAlpha
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Radio(
    isSelected: Boolean,
    onSelectedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    RadioButton(
        selected = isSelected,
        onClick = { onSelectedChanged(!isSelected) },
        modifier = modifier,
        enabled = enabled,
        colors = RadioButtonDefaults.colors(
            selectedColor = AppTheme.colors.primary,
            unselectedColor = AppTheme.colors.medium,
            disabledColor = AppTheme.colors.medium.copy(alpha = ContentAlpha.disabled),
        )
    )
}

@Preview(name = "Not checked", group = "Radio")
@Composable
private fun RadioPreview_NotChecked() {
    AppTheme {
        AppSurface {
            Radio(
                isSelected = false,
                onSelectedChanged = {},
            )
        }
    }
}

@Preview(name = "Checked", group = "Radio")
@Composable
private fun RadioPreview_IsChecked() {
    AppTheme {
        AppSurface {
            Radio(
                isSelected = true,
                onSelectedChanged = {},
            )
        }
    }
}

@Preview(name = "Not checked not enabled", group = "Radio")
@Composable
private fun RadioPreview_NotChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            Radio(
                isSelected = false,
                onSelectedChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Checked not enabled", group = "Radio")
@Composable
private fun RadioPreview_IsChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            Radio(
                isSelected = true,
                onSelectedChanged = {},
                enabled = false,
            )
        }
    }
}
