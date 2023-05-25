package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.PrimarySwitch
import com.blockchain.componentlib.control.SuccessSwitch
import com.blockchain.componentlib.theme.AppTheme

@Composable
private fun ToggleTableRow(
    paddingValues: PaddingValues,
    onCheckedChange: (isChecked: Boolean) -> Unit,
    primaryText: String,
    secondaryText: String = "",
    isChecked: Boolean = false,
    enabled: Boolean = true,
    toggleTableRowType: ToggleTableRowType = ToggleTableRowType.Primary,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary
) {
    FlexibleTableRow(
        paddingValues = paddingValues,
        content = {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryText,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )
                if (secondaryText.isNotBlank()) {
                    Text(
                        text = secondaryText,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                }
            }
        },
        contentEnd = {
            when (toggleTableRowType) {
                ToggleTableRowType.Primary -> {
                    PrimarySwitch(
                        isChecked = isChecked,
                        onCheckChanged = onCheckedChange,
                        enabled = enabled
                    )
                }
                ToggleTableRowType.Success -> {
                    SuccessSwitch(
                        isChecked = isChecked,
                        onCheckChanged = onCheckedChange,
                        enabled = enabled
                    )
                }
            }
        },
        backgroundColor = backgroundColor
    )
}

@Composable
fun ToggleTableRow(
    onCheckedChange: (isChecked: Boolean) -> Unit,
    primaryText: String,
    secondaryText: String = "",
    isChecked: Boolean = false,
    enabled: Boolean = true,
    toggleTableRowType: ToggleTableRowType = ToggleTableRowType.Primary,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary
) {
    ToggleTableRow(
        paddingValues = PaddingValues(
            start = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
            // Switch has a built-in padding and we need to consider it for the screen padding
            end = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing),
            top = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing),
            bottom = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing)
        ),
        onCheckedChange = onCheckedChange,
        primaryText = primaryText,
        secondaryText = secondaryText,
        isChecked = isChecked,
        enabled = enabled,
        toggleTableRowType = toggleTableRowType,
        backgroundColor = backgroundColor
    )
}

@Composable
fun FlexibleToggleTableRow(
    paddingValues: PaddingValues = PaddingValues(
        start = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
        // Switch has a built-in padding and we need to consider it for the screen padding
        end = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing),
        top = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing),
        bottom = dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing)
    ),
    onCheckedChange: (isChecked: Boolean) -> Unit,
    primaryText: String,
    secondaryText: String = "",
    isChecked: Boolean = false,
    enabled: Boolean = true,
    toggleTableRowType: ToggleTableRowType = ToggleTableRowType.Primary,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary
) {
    ToggleTableRow(
        paddingValues = paddingValues,
        onCheckedChange = onCheckedChange,
        primaryText = primaryText,
        secondaryText = secondaryText,
        isChecked = isChecked,
        enabled = enabled,
        toggleTableRowType = toggleTableRowType,
        backgroundColor = backgroundColor
    )
}

enum class ToggleTableRowType { Primary, Success }

@Preview
@Composable
private fun FlexibleToggleTableRow_SingleLine_NotChecked() {
    AppTheme {
        Surface {
            FlexibleToggleTableRow(
                paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
                onCheckedChange = {},
                primaryText = "Enable this ?",
                backgroundColor = Color.Red
            )
        }
    }
}

@Preview
@Composable
private fun ToggleTableRow_SingleLine_NotChecked() {
    AppTheme {
        Surface {
            ToggleTableRow(
                onCheckedChange = {},
                primaryText = "Enable this ?"
            )
        }
    }
}

@Preview
@Composable
private fun ToggleTableRow_SingleLine_Checked() {
    AppTheme {
        Surface {
            ToggleTableRow(
                onCheckedChange = {},
                primaryText = "Enable this ?",
                isChecked = true
            )
        }
    }
}

@Preview
@Composable
private fun ToggleTableRow_NotChecked() {
    AppTheme {
        Surface {
            ToggleTableRow(
                onCheckedChange = {},
                primaryText = "Enable this ?",
                secondaryText = "Some additional info"
            )
        }
    }
}

@Preview
@Composable
private fun ToggleTableRow_Checked() {
    AppTheme {
        Surface {
            ToggleTableRow(
                onCheckedChange = {},
                primaryText = "Enable this ?",
                secondaryText = "Some additional info",
                isChecked = true
            )
        }
    }
}

@Preview
@Composable
private fun ToggleTableRow_SingleLine_NotChecked_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            ToggleTableRow(
                onCheckedChange = {},
                primaryText = "Enable this ?"
            )
        }
    }
}

@Preview
@Composable
private fun ToggleTableRow_NotChecked_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            ToggleTableRow(
                onCheckedChange = {},
                primaryText = "Enable this ?",
                secondaryText = "Some additional info"
            )
        }
    }
}

@Preview
@Composable
private fun ToggleTableRow_Checked_Dark() {
    AppTheme(darkTheme = true) {
        Surface {
            ToggleTableRow(
                onCheckedChange = {},
                primaryText = "Enable this ?",
                secondaryText = "Some additional info",
                isChecked = true
            )
        }
    }
}
