package com.blockchain.componentlib.keyboard

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class PinKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var onClick by mutableStateOf({ _: KeyboardButton -> })
    var bgColor: Color? by mutableStateOf(null)
    var withBiometrics: Boolean by mutableStateOf(false)

    @Composable
    override fun Content() {
        PinKeyboard(
            withBiometrics = withBiometrics,
            backgroundColor = bgColor ?: AppTheme.colors.background,
            onClick = onClick
        )
    }
}
