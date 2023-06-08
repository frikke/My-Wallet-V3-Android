package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.common.BaseButtonView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600

class MinimalPrimaryButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                MinimalPrimaryButton(
                    onClick = onClick,
                    text = text,
                    state = buttonState,
                    icon = icon as? ImageResource.Local
                )
            }
        }
    }
}
