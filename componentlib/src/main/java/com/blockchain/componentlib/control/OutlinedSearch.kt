package com.blockchain.componentlib.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect

@Composable
private fun OutlinedSearch(
    prePopulatedText: String = "",
    placeholder: String = "",
    readOnly: Boolean = false,
    showCloseButton: Boolean = false,
    onValueChange: (String) -> Unit = {}
) {

    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(prePopulatedText) }

    val searchIcon = ImageResource.Local(R.drawable.ic_search, null)
    val closeIcon = if (isSystemInDarkTheme()) {
        ImageResource.Local(R.drawable.ic_close_circle_dark, null)
    } else {
        ImageResource.Local(R.drawable.ic_close_circle, null)
    }

    fun clearText() {
        onValueChange.invoke("")
        value = ""
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextInput(
            modifier = Modifier.weight(1F),
            shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium),
            value = value,
            onValueChange = {
                onValueChange.invoke(it)
                value = it
            },
            state = TextInputState.Default(defaultMessage = null),
            label = null,
            placeholder = placeholder,
            singleLine = true,
            unfocusedTrailingIcon = searchIcon,
            focusedTrailingIcon = closeIcon,
            onTrailingIconClicked = {
                clearText()
            },
            onFocusChanged = {
                isFocused = it.isFocused
            },
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus(true) }
            ),
            readOnly = readOnly
        )

        AnimatedVisibility(visible = showCloseButton && isFocused) {
            Row {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                Text(
                    modifier = Modifier
                        .clickableNoEffect {
                            clearText()
                            focusManager.clearFocus(true)
                        },
                    text = stringResource(R.string.common_cancel),
                    style = AppTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    color = AppTheme.colors.primary,
                )
            }
        }
    }
}

@Composable
fun NonCancelableOutlinedSearch(
    prePopulatedText: String = "",
    placeholder: String = "",
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit = {},
) {
    OutlinedSearch(
        prePopulatedText = prePopulatedText,
        placeholder = placeholder,
        readOnly = readOnly,
        showCloseButton = false,
        onValueChange = onValueChange
    )
}

@Composable
fun CancelableOutlinedSearch(
    prePopulatedText: String = "",
    placeholder: String = "",
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit = {}
) {
    OutlinedSearch(
        prePopulatedText = prePopulatedText,
        placeholder = placeholder,
        readOnly = readOnly,
        showCloseButton = true,
        onValueChange = onValueChange
    )
}

@Preview
@Composable
private fun SearchPreview() {
    AppTheme {
        AppSurface {
            Search(
                onValueChange = {
                }
            )
        }
    }
}
