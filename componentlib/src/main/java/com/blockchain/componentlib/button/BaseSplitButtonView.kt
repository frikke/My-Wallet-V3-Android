package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

abstract class BaseSplitButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onStartButtonClick by mutableStateOf({})
    var startButtonText by mutableStateOf("")
    var startButtonState by mutableStateOf(ButtonState.Enabled)

    var onEndButtonClick by mutableStateOf({})
    var endButtonText by mutableStateOf("")
    var endButtonState by mutableStateOf(ButtonState.Enabled)
}
