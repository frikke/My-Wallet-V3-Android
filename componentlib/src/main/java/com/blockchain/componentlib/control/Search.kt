package com.blockchain.componentlib.control

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.controls.TextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Search(
    prePopulatedText: String = "",
    label: String = "",
    placeholder: String = "",
    readOnly: Boolean = false,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    onValueChange: (String) -> Unit = {},
    clearInput: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(prePopulatedText) }
    var shouldClearInput by remember { mutableStateOf(clearInput) }

    val searchIcon = ImageResource.Local(R.drawable.ic_search, null)
    val closeIcon = if (isDarkMode) {
        ImageResource.Local(R.drawable.ic_close_circle_dark, null)
    } else {
        ImageResource.Local(R.drawable.ic_close_circle, null)
    }

    val trailingIcon = if (isFocused) {
        closeIcon
    } else {
        searchIcon
    }

    TextInput(
        value = if (shouldClearInput) {
            onValueChange.invoke("")
            focusManager.clearFocus(true)
            value = ""
            ""
        } else {
            value
        },
        onValueChange = {
            onValueChange.invoke(it)
            value = it
        },
        state = TextInputState.Default(defaultMessage = null),
        label = label,
        placeholder = placeholder,
        singleLine = true,
        trailingIcon = if (!readOnly) trailingIcon else ImageResource.None,
        onTrailingIconClicked = {
            onValueChange.invoke("")
            value = ""
            focusManager.clearFocus(true)
        },
        onFocusChanged = {
            isFocused = it.isFocused
        },
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus(true) }
        ),
        readOnly = readOnly
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
