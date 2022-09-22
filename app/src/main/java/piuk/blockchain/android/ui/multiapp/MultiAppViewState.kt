package piuk.blockchain.android.ui.multiapp

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode
import piuk.blockchain.android.R

data class MultiAppViewState(
    val modeSwitcherOptions: List<WalletMode>,
    val selectedMode: WalletMode,
    val backgroundColors: ChromeBackgroundColors,
    val totalBalance: DataResource<String>,
    val bottomNavigationItems: List<ChromeBottomNavigationItem>
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

sealed interface ChromeBottomNavigationItem {
    @get:StringRes
    val name: Int

    @get:DrawableRes
    val iconDefault: Int

    @get:DrawableRes
    val iconSelected: Int

    val route: String

    object Home : ChromeBottomNavigationItem {
        override val name: Int = R.string.chrome_navigation_home
        override val iconDefault: Int = R.drawable.ic_chrome_home_default
        override val iconSelected: Int = R.drawable.ic_chrome_home_selected
        override val route: String = "home"
    }

    object Trade : ChromeBottomNavigationItem {
        override val name: Int = R.string.chrome_navigation_trade
        override val iconDefault: Int = R.drawable.ic_chrome_trade_default
        override val iconSelected: Int = R.drawable.ic_chrome_trade_selected
        override val route: String = "trade"
    }

    object Card : ChromeBottomNavigationItem {
        override val name: Int = R.string.chrome_navigation_card
        override val iconDefault: Int = R.drawable.ic_chrome_card_default
        override val iconSelected: Int = R.drawable.ic_chrome_card_selected
        override val route: String = "card"
    }

    object Nft : ChromeBottomNavigationItem {
        override val name: Int = R.string.chrome_navigation_nft
        override val iconDefault: Int = R.drawable.ic_chrome_nft_default
        override val iconSelected: Int = R.drawable.ic_chrome_nft_selected
        override val route: String = "nft"
    }
}
