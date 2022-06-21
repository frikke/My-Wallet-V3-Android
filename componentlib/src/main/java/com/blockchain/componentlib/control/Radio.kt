package com.blockchain.componentlib.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Red000
import com.blockchain.componentlib.theme.Red900

@Composable
fun NoPaddingRadio(
    modifier: Modifier = Modifier,
    state: RadioButtonState,
    onSelectedChanged: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    Radio(
        state = state,
        onSelectedChanged = onSelectedChanged,
        modifier = modifier,
        enabled = enabled,
        isDarkMode = isDarkMode,
        withPadding = false
    )
}

@Composable
fun Radio(
    state: RadioButtonState,
    onSelectedChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    Radio(
        state = state,
        onSelectedChanged = onSelectedChanged,
        modifier = modifier,
        enabled = enabled,
        isDarkMode = isDarkMode,
        withPadding = true
    )
}

@Composable
private fun Radio(
    state: RadioButtonState,
    onSelectedChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    withPadding: Boolean = true,
) {

    val selectedColor = AppTheme.colors.primary
    val unselectedColor = AppTheme.colors.medium
    val errorColor = AppTheme.colors.error
    val errorFillColor = if (isDarkMode) Red900 else Red000

    var radioRingColor by remember(
        state,
        selectedColor,
        unselectedColor,
        errorColor,
        errorFillColor
    ) {
        mutableStateOf(
            when (state) {
                RadioButtonState.Selected -> selectedColor
                RadioButtonState.Unselected -> unselectedColor
                RadioButtonState.Error -> errorColor
            }
        )
    }

    var radioCenterColor by remember(
        state,
        selectedColor,
        unselectedColor,
        errorColor,
        errorFillColor
    ) {
        mutableStateOf(
            when (state) {
                RadioButtonState.Selected -> selectedColor
                RadioButtonState.Unselected -> unselectedColor
                RadioButtonState.Error -> Color.Transparent
            }
        )
    }

    var radioFillColor by remember(
        state,
        selectedColor,
        unselectedColor,
        errorColor,
        errorFillColor
    ) {
        mutableStateOf(
            when (state) {
                RadioButtonState.Selected -> Color.Transparent
                RadioButtonState.Unselected -> Color.Transparent
                RadioButtonState.Error -> errorFillColor
            }
        )
    }

    Box(
        modifier = modifier
            .run {
                if (!enabled) {
                    this.alpha(ContentAlpha.disabled)
                } else if (onSelectedChanged != null) {
                    this.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(
                            bounded = false,
                            radius = dimensionResource(R.dimen.standard_margin),
                        ),
                    ) {
                        onSelectedChanged(
                            when (state) {
                                RadioButtonState.Selected -> false
                                RadioButtonState.Unselected -> true
                                RadioButtonState.Error -> true
                            }
                        )
                    }
                } else {
                    this
                }
            }
            .padding(dimensionResource(if (withPadding) R.dimen.very_small_margin else R.dimen.zero_margin))
            .size(dimensionResource(R.dimen.standard_margin))
            .background(
                color = animateColorAsState(targetValue = radioFillColor).value,
                shape = CircleShape,
            )
            .border(
                width = 2.dp,
                color = animateColorAsState(targetValue = radioRingColor).value,
                shape = CircleShape
            )
            .padding(5.dp)
            .background(
                color = animateColorAsState(targetValue = radioCenterColor).value,
                shape = CircleShape,
            ),
    )
}

enum class RadioButtonState {
    Selected, Unselected, Error
}

@Preview(name = "Not checked", group = "Radio")
@Composable
private fun RadioPreview_NotChecked() {
    AppTheme {
        AppSurface {
            Radio(
                state = RadioButtonState.Unselected,
                onSelectedChanged = {},
            )
        }
    }
}

@Preview(name = "Error", group = "Radio")
@Composable
private fun RadioPreview_Error() {
    AppTheme {
        AppSurface {
            Radio(
                state = RadioButtonState.Error,
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
                state = RadioButtonState.Selected,
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
                state = RadioButtonState.Unselected,
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
                state = RadioButtonState.Selected,
                onSelectedChanged = {},
                enabled = false,
            )
        }
    }
}
