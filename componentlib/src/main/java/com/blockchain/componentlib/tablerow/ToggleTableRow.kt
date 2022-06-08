package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.control.PrimarySwitch
import com.blockchain.componentlib.control.SuccessSwitch
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ToggleTableRow(
    onCheckedChange: (isChecked: Boolean) -> Unit,
    primaryText: String,
    secondaryText: String = "",
    isChecked: Boolean = false,
    enabled: Boolean = true,
    toggleTableRowType: ToggleTableRowType = ToggleTableRowType.Primary,
) {
    FlexibleTableRow(
        paddingValues = PaddingValues(
            start = dimensionResource(R.dimen.standard_margin),
            end = 18.dp, // Switch has a built-in padding and we need to consider it for the screen padding
            top = dimensionResource(R.dimen.medium_margin),
            bottom = dimensionResource(R.dimen.medium_margin),
        ),
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
                        enabled = enabled,
                    )
                }
                ToggleTableRowType.Success -> {
                    SuccessSwitch(
                        isChecked = isChecked,
                        onCheckChanged = onCheckedChange,
                        enabled = enabled,
                    )
                }
            }
        }
    )
}

enum class ToggleTableRowType { Primary, Success }

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
