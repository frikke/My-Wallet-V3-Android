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
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class OutlinedTextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var value by mutableStateOf("")
    var onValueChange by mutableStateOf({ _: String -> })
    var state: TextInputState by mutableStateOf(TextInputState.Default())
    var labelText by mutableStateOf("")
    var placeholderText by mutableStateOf("")
    var unfocusedTrailingIconResource: ImageResource by mutableStateOf(ImageResource.None)
    var focusedTrailingIconResource: ImageResource by mutableStateOf(ImageResource.None)
    var leadingIconResource: ImageResource by mutableStateOf(ImageResource.None)
    var singleLine by mutableStateOf(false)
    var inputType by mutableStateOf(KeyboardType.Text)
    var onTrailingIconClicked by mutableStateOf({})
    var maxLength by mutableStateOf(Int.MAX_VALUE)

    @Composable
    override fun Content() {
        if (isInEditMode) {
            labelText = "dummy label"
            value = "dummy text"
        }

        OutlinedTextInput(
            value = value,
            onValueChange = {
                value = it
                onValueChange(it)
            },
            state = state,
            placeholder = placeholderText,
            label = labelText,
            unfocusedTrailingIcon = unfocusedTrailingIconResource,
            focusedTrailingIcon = focusedTrailingIconResource,
            leadingIcon = leadingIconResource,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = inputType),
            onTrailingIconClicked = onTrailingIconClicked,
            maxLength = maxLength
        )
    }

    fun clearState() {
        value = ""
        onValueChange = { _: String -> }
        state = TextInputState.Default()
        labelText = ""
        placeholderText = ""
        unfocusedTrailingIconResource = ImageResource.None
        focusedTrailingIconResource = ImageResource.None
        leadingIconResource = ImageResource.None
        onTrailingIconClicked = {}
    }
}
