package com.blockchain.componentlib.button.common

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.utils.BaseAbstractComposeView

abstract class BaseButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var onClick by mutableStateOf({})
    var text by mutableStateOf("")
    var buttonState by mutableStateOf(ButtonState.Enabled)
    var icon: ImageResource by mutableStateOf(ImageResource.None)

    fun clearState() {
        onClick = {}
        text = ""
        buttonState = ButtonState.Enabled
        icon = ImageResource.None
    }
}
