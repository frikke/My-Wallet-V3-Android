package com.blockchain.componentlib.sectionheader

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class WalletBalanceSectionHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var primaryText by mutableStateOf("")
    var secondaryText by mutableStateOf("")
    var buttonText by mutableStateOf("")
    var onButtonClick by mutableStateOf({})
    var buttonState by mutableStateOf(ButtonState.Enabled)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                WalletBalanceSectionHeader(
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    buttonText = buttonText,
                    onButtonClick = onButtonClick,
                    buttonState = buttonState,
                )
            }
        }
    }
}
