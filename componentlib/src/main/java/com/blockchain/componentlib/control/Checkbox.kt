package com.blockchain.componentlib.control

import android.content.res.Configuration
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Checkbox(
    state: CheckboxState,
    onCheckChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val checkboxCheckedFillColor = AppTheme.colors.primary
    val checkboxCheckedBorderColor = AppTheme.colors.primary

    val checkboxUncheckedFillColor = AppTheme.colors.light
    val checkboxUncheckedBorderColor = AppTheme.colors.medium

    val checkboxErrorFillColor = AppTheme.colors.errorLight
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
                } else if (onCheckChanged != null) {
                    this.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(
                            bounded = false,
                            radius = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
                        )
                    ) {
                        onCheckChanged(
                            when (state) {
                                CheckboxState.Checked -> false
                                CheckboxState.Unchecked -> true
                                CheckboxState.Error -> true
                            }
                        )
                    }
                } else {
                    this
                }
            }
            .size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
            .background(
                color = animateColorAsState(targetValue = checkboxBorderColor).value,
                shape = RoundedCornerShape(dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing))
            )
            .padding(2.dp)
            .background(
                color = animateColorAsState(targetValue = checkboxFillColor).value,
                shape = RoundedCornerShape(2.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageResource = Icons.Check.withTint(AppColors.titleSecondary),
            modifier = Modifier.alpha(
                animateFloatAsState(targetValue = checkboxAlpha).value
            )
        )
    }
}

enum class CheckboxState {
    Checked, Unchecked, Error
}

@Preview
@Composable
private fun CheckboxPreview_NotChecked() {
    Checkbox(
        state = CheckboxState.Unchecked,
        onCheckChanged = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CheckboxPreview_NotCheckedDark() {
    CheckboxPreview_NotChecked()
}

@Preview
@Composable
private fun CheckboxPreview_IsChecked() {
    Checkbox(
        state = CheckboxState.Checked,
        onCheckChanged = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CheckboxPreview_IsCheckedDark() {
    CheckboxPreview_IsChecked()
}

@Preview
@Composable
private fun CheckboxPreview_Error() {
    Checkbox(
        state = CheckboxState.Error,
        onCheckChanged = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CheckboxPreview_ErrorDark() {
    CheckboxPreview_Error()
}

@Preview
@Composable
private fun CheckboxPreview_NotChecked_NotEnabled() {
    Checkbox(
        state = CheckboxState.Unchecked,
        onCheckChanged = {},
        enabled = false
    )
}

@Preview
@Composable
private fun CheckboxPreview_IsChecked_NotEnabled() {
    Checkbox(
        state = CheckboxState.Checked,
        onCheckChanged = {},
        enabled = false
    )
}
