package com.blockchain.componentlib.controls

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.core.content.res.ResourcesCompat

class TextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var value by mutableStateOf("")
    var onValueChange by mutableStateOf({ _: String -> })
    var isInputEnabled by mutableStateOf(true)
    var isError by mutableStateOf(false)
    var assistiveText by mutableStateOf("")
    var errorText by mutableStateOf("")
    var labelText by mutableStateOf("")
    var placeholderText by mutableStateOf("")
    var trailingIcon by mutableStateOf(ResourcesCompat.ID_NULL)
    var leadingIcon by mutableStateOf(ResourcesCompat.ID_NULL)

    @Composable
    override fun Content() {
        TextInput(
            value = value,
            onValueChange = onValueChange,
            enabled = isInputEnabled,
            assistiveText = assistiveText,
            placeholder = placeholderText,
            label = labelText,
            errorMessage = errorText,
            isError = isError,
            trailingIcon = trailingIcon,
            leadingIcon = leadingIcon
        )
    }
}
