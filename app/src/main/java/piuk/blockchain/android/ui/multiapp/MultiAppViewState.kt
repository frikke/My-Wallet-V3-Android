package piuk.blockchain.android.ui.multiapp

import androidx.compose.ui.graphics.Color
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode

data class MultiAppViewState(
    val modeSwitcherOptions: List<WalletMode>,
    val selectedMode: WalletMode,
    val backgroundColors: ChromeBackgroundColors,
    val totalBalance: DataResource<String>,
) : ViewState

sealed interface ChromeBackgroundColors {
    val startColor: Color
    val endColor: Color

    object Trading : ChromeBackgroundColors {
        override val startColor: Color get() = START_TRADING
        override val endColor: Color get() = END_TRADING
    }

    object DeFi : ChromeBackgroundColors {
        override val startColor: Color get() = START_DEFI
        override val endColor: Color get() = END_DEFI
    }
}
