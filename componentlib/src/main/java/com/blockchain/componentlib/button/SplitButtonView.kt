package com.blockchain.componentlib.button

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class SplitButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseSplitButtonView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                SplitButtons(
                    primaryButtonText = primaryButtonText,
                    primaryButtonOnClick = onPrimaryButtonClick,
                    secondaryButtonText = secondaryButtonText,
                    secondaryButtonOnClick = onSecondaryButtonClick,
                    primaryButtonState = primaryButtonState,
                    secondaryButtonState = secondaryButtonState,
                    primaryButtonAlignment = primaryButtonAlignment
                )
            }
        }
    }
}
