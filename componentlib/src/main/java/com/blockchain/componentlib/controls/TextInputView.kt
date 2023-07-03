package com.blockchain.componentlib.controls

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class TextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var value by mutableStateOf("")
    var onValueChange by mutableStateOf({ _: String -> })
    var state: TextInputState by mutableStateOf(TextInputState.Default())
    var labelText by mutableStateOf("")
    var placeholderText by mutableStateOf("")
    var trailingIconResource: ImageResource by mutableStateOf(ImageResource.None)
    var leadingIconResource: ImageResource by mutableStateOf(ImageResource.None)
    var singleLine by mutableStateOf(false)
    var inputType by mutableStateOf(KeyboardType.Text)
    var onTrailingIconClicked by mutableStateOf({})

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                TextInput(
                    value = value,
                    onValueChange = {
                        value = it
                        onValueChange(it)
                    },
                    state = state,
                    placeholder = placeholderText,
                    label = labelText,
                    trailingIcon = trailingIconResource,
                    leadingIcon = leadingIconResource,
                    singleLine = singleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = inputType),
                    onTrailingIconClicked = onTrailingIconClicked
                )
            }
        }
    }

    fun clearState() {
        value = ""
        onValueChange = { _: String -> }
        state = TextInputState.Default()
        labelText = ""
        placeholderText = ""
        trailingIconResource = ImageResource.None
        leadingIconResource = ImageResource.None
        onTrailingIconClicked = {}
    }
}
