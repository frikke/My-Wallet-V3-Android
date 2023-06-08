package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.common.BaseButtonView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class SecondaryButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseButtonView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                SecondaryButton(
                    onClick = onClick,
                    text = text,
                    state = buttonState,
                    icon = icon as? ImageResource.Local
                )
            }
        }
    }
}
