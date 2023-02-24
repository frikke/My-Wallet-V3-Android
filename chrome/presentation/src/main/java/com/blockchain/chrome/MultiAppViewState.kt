package com.blockchain.chrome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode

data class MultiAppViewState(
    val modeSwitcherOptions: ChromeModeOptions?,
    val selectedMode: WalletMode?,
    val backgroundColors: ChromeBackgroundColors?,
    val totalBalance: DataResource<String>,
    val shouldRevealBalance: Boolean,
    val bottomNavigationItems: List<ChromeBottomNavigationItem>?
) : ViewState

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

    object Prices : ChromeBottomNavigationItem {
        override val name: Int = R.string.chrome_navigation_prices
        override val iconDefault: Int = R.drawable.ic_chrome_trade_default
        override val iconSelected: Int = R.drawable.ic_chrome_trade_selected
        override val route: String = "trade"
    }

    object Nft : ChromeBottomNavigationItem {
        override val name: Int = R.string.chrome_navigation_nft
        override val iconDefault: Int = R.drawable.ic_chrome_nft_default
        override val iconSelected: Int = R.drawable.ic_chrome_nft_selected
        override val route: String = "nft"
    }

    object Dex : ChromeBottomNavigationItem {
        override val name: Int = R.string.chrome_navigation_dex
        override val iconDefault: Int = R.drawable.ic_chrome_dex_default
        override val iconSelected: Int = R.drawable.ic_chrome_dex_selected
        override val route: String = "dex"
    }

    object Earn : ChromeBottomNavigationItem {
        override val name: Int = R.string.chrome_navigation_earn
        override val iconDefault: Int = R.drawable.ic_chrome_earn_default
        override val iconSelected: Int = R.drawable.ic_chrome_earn_selected
        override val route: String = "earn"
    }
}
