package com.blockchain.componentlib.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.White

@Composable
fun NoPaddingRadio(
    modifier: Modifier = Modifier,
    state: RadioButtonState,
    onSelectedChanged: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    Radio(
        state = state,
        onSelectedChanged = onSelectedChanged,
        modifier = modifier,
        enabled = enabled,
        withPadding = false
    )
}

@Composable
fun Radio(
    state: RadioButtonState,
    onSelectedChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Radio(
        state = state,
        onSelectedChanged = onSelectedChanged,
        modifier = modifier,
        enabled = enabled,
        withPadding = true
    )
}

@Composable
private fun Radio(
    state: RadioButtonState,
    onSelectedChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    withPadding: Boolean = true
) {
    val selectedColor = AppTheme.colors.primary
    val unselectedColor = AppTheme.colors.medium
    val errorColor = AppTheme.colors.error
    val errorFillColor = AppTheme.colors.errorLight

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
                            radius = dimensionResource(R.dimen.standard_spacing)
                        )
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
            .padding(
                dimensionResource(
                    if (withPadding) R.dimen.very_small_spacing else R.dimen.zero_spacing
                )
            )
            .size(dimensionResource(R.dimen.standard_spacing))
            .background(
                color = animateColorAsState(targetValue = radioFillColor).value,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = animateColorAsState(targetValue = radioRingColor).value,
                shape = CircleShape
            )
            .padding(5.dp)
            .background(
                color = animateColorAsState(targetValue = radioCenterColor).value,
                shape = CircleShape
            )
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
                onSelectedChanged = {}
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
                onSelectedChanged = {}
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
                onSelectedChanged = {}
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
                enabled = false
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
                enabled = false
            )
        }
    }
}

@Composable
fun RadioCheckMark(state: RadioButtonState, onSelectedChanged: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(AppTheme.dimensions.standardSpacing)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = AppTheme.dimensions.standardSpacing),
                onClick = onSelectedChanged
            ),
        contentAlignment = Alignment.Center
    ) {
        if (state == RadioButtonState.Selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(AppTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageResource = ImageResource.Local(
                        R.drawable.ic_check_green,
                        colorFilter = ColorFilter.tint(White)
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppTheme.dimensions.smallestSpacing)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = AppTheme.dimensions.composeSmallestSpacing,
                        color = Grey100,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview
@Composable
fun CircleCheckboxPreview() {
    // Use interactive mode to preview the animation

    var isChecked by remember { mutableStateOf(false) }

    AppTheme {
        AppSurface {
            RadioCheckMark(
                state = if (isChecked) RadioButtonState.Selected else RadioButtonState.Unselected,
                onSelectedChanged = {
                    isChecked = !isChecked
                }
            )
        }
    }
}
