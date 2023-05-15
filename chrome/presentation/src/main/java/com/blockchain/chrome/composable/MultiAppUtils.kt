package com.blockchain.chrome.composable

import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.walletmode.WalletMode
import java.util.concurrent.TimeUnit

const val ANIMATION_DURATION = 400
val REVEAL_DELAY_MS = TimeUnit.SECONDS.toMillis(3)

fun WalletMode.bottomNavigationItems(): List<ChromeBottomNavigationItem> = when (this) {
    WalletMode.CUSTODIAL -> listOf(
        ChromeBottomNavigationItem.Home,
        ChromeBottomNavigationItem.Prices,
        ChromeBottomNavigationItem.Earn
    )
    WalletMode.NON_CUSTODIAL -> listOf(
        ChromeBottomNavigationItem.Home,
        ChromeBottomNavigationItem.Prices,
        ChromeBottomNavigationItem.Nft,
        ChromeBottomNavigationItem.Dex
    )
}
