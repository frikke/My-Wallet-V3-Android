package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.button.common.BaseButtonView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600

class MinimalButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    var textColor: Color by mutableStateOf(Blue600)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                MinimalButton(
                    onClick = onClick,
                    text = text,
                    textColor = textColor,
                    state = buttonState,
                    icon = icon
                )
            }
        }
    }
}
