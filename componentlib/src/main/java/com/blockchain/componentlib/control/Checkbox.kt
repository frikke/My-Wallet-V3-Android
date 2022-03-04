package com.blockchain.componentlib.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Red000
import com.blockchain.componentlib.theme.Red900

@Composable
fun Checkbox(
    state: CheckboxState,
    onCheckChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
) {

    val checkboxCheckedFillColor = AppTheme.colors.primary
    val checkboxCheckedBorderColor = AppTheme.colors.primary

    val checkboxUncheckedFillColor = AppTheme.colors.light
    val checkboxUncheckedBorderColor = AppTheme.colors.medium

    val checkboxErrorFillColor = if (isDarkTheme) Red900 else Red000
    val checkboxErrorBorderColor = AppTheme.colors.error

    val checkboxFillColor by remember(
        state,
        checkboxCheckedFillColor,
        checkboxUncheckedFillColor,
        checkboxErrorFillColor
    ) {
        mutableStateOf(
            when (state) {
                CheckboxState.Checked -> checkboxCheckedFillColor
                CheckboxState.Unchecked -> checkboxUncheckedFillColor
                CheckboxState.Error -> checkboxErrorFillColor
            }
        )
    }

    val checkboxBorderColor by remember(
        state,
        checkboxCheckedBorderColor,
        checkboxUncheckedBorderColor,
        checkboxErrorBorderColor
    ) {
        mutableStateOf(
            when (state) {
                CheckboxState.Checked -> checkboxCheckedBorderColor
                CheckboxState.Unchecked -> checkboxUncheckedBorderColor
                CheckboxState.Error -> checkboxErrorBorderColor
            }
        )
    }

    val checkboxAlpha by remember(state) {
        mutableStateOf(
            if (state == CheckboxState.Checked) 1f else 0f
        )
    }

    Box(
        modifier = modifier
            .run {
                if (!enabled) {
                    this.alpha(ContentAlpha.disabled)
                } else {
                    this.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(
                            bounded = false,
                            radius = 24.dp,
                        ),
                    ) {
                        onCheckChanged(
                            when (state) {
                                CheckboxState.Checked -> false
                                CheckboxState.Unchecked -> true
                                CheckboxState.Error -> true
                            }
                        )
                    }
                }
            }
            .padding(12.dp)
            .size(24.dp)
            .background(
                color = animateColorAsState(targetValue = checkboxBorderColor).value,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(2.dp)
            .background(
                color = animateColorAsState(targetValue = checkboxFillColor).value,
                shape = RoundedCornerShape(2.dp),
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageResource = ImageResource.Local(
                id = if (isDarkTheme) R.drawable.ic_check_dark else R.drawable.ic_check_light,
                contentDescription = null,
            ),
            modifier = Modifier.alpha(
                animateFloatAsState(targetValue = checkboxAlpha).value
            )
        )
    }
}

enum class CheckboxState {
    Checked, Unchecked, Error
}

@Preview(name = "Not checked", group = "Checkbox")
@Composable
private fun CheckboxPreview_NotChecked() {
    AppTheme {
        AppSurface {
            Checkbox(
                state = CheckboxState.Unchecked,
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
                state = CheckboxState.Checked,
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
                state = CheckboxState.Unchecked,
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
                state = CheckboxState.Checked,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}
