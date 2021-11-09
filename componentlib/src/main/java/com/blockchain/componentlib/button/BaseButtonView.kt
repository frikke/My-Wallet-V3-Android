package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

abstract class BaseButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onClick by mutableStateOf({})
    var text by mutableStateOf("")
    var buttonState by mutableStateOf(ButtonState.Enabled)
}
