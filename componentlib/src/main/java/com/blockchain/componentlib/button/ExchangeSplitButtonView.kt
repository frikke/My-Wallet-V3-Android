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
        AppTheme {
            AppSurface {
                ExchangeSplitButtons(
                    exchangeBuyButtonText = startButtonText,
                    exchangeBuyButtonOnClick = onStartButtonClick,
                    exchangeSellButtonText = endButtonText,
                    exchangeSellButtonOnClick = onEndButtonClick,
                    exchangeBuyButtonState = startButtonState,
                    exchangeSellButtonState = endButtonState,
                )
            }
        }
    }
}
