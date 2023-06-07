package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.button.common.BaseButtonView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class SmallMinimalButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    var isTransparent by mutableStateOf(false)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                SmallMinimalButton(
                    onClick = onClick,
                    text = text,
                    state = buttonState,
                    icon = icon,
                    isTransparent = isTransparent
                )
            }
        }
    }
}
