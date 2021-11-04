package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView

class MinimalButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onClick = mutableStateOf({})
    var text = mutableStateOf("")
    var buttonState = mutableStateOf(MinimalButtonState.Enabled)
    var isMaxWidth = mutableStateOf(false)

    @Composable
    override fun Content() {
        MinimalButton(
            onClick = onClick.value,
            text = text.value,
            state = buttonState.value,
            modifier = if (isMaxWidth.value) Modifier.fillMaxWidth() else Modifier
        )
    }
}
