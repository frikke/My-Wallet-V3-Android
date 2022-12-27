package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class ExchangeSplitButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseSplitButtonView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                ExchangeSplitButtons(
                    exchangeBuyButtonText = primaryButtonText,
                    exchangeBuyButtonOnClick = onPrimaryButtonClick,
                    exchangeSellButtonText = secondaryButtonText,
                    exchangeSellButtonOnClick = onSecondaryButtonClick,
                    exchangeBuyButtonState = primaryButtonState,
                    exchangeSellButtonState = secondaryButtonState,
                )
            }
        }
    }
}
