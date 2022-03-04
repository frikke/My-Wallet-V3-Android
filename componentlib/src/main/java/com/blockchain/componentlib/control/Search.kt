package com.blockchain.componentlib.control

import androidx.compose.foundation.isSystemInDarkTheme
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
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Search(
    onValueChange: (String) -> Unit = {},
    label: String = "",
    isDarkMode: Boolean = isSystemInDarkTheme()
) {

    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }

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
        value = value,
        onValueChange = {
            onValueChange.invoke(it)
            value = it
        },
        label = label,
        placeholder = label,
        trailingIcon = trailingIcon,
        onTrailingIconClicked = {
            value = ""
            focusManager.clearFocus(true)
        },
        onFocusChanged = {
            isFocused = it.isFocused
        }
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
