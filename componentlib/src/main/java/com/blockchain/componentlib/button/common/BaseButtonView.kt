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

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        onClick = { l?.onClick(this) }
    }

    override fun setEnabled(enabled: Boolean) {
        buttonState = if (enabled) ButtonState.Enabled else ButtonState.Disabled
    }

    override fun isEnabled(): Boolean = buttonState == ButtonState.Enabled

    fun clearState() {
        onClick = {}
        text = ""
        buttonState = ButtonState.Enabled
        icon = ImageResource.None
    }
}
