package com.blockchain.chrome.composable

import androidx.annotation.StringRes
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.walletmode.WalletMode
import java.util.concurrent.TimeUnit

const val ANIMATION_DURATION = 400
val REVEAL_DELAY_MS = TimeUnit.SECONDS.toMillis(3)

@StringRes
fun WalletMode.titleSuperApp(): Int = when (this) {
    WalletMode.CUSTODIAL -> R.string.brokerage_wallet_name
    WalletMode.NON_CUSTODIAL -> R.string.defi_wallet_name
}

fun WalletMode.titleIcon(): ImageResource = when (this) {
    WalletMode.CUSTODIAL -> ImageResource.Local(R.drawable.ic_brokerage_logo)
    WalletMode.NON_CUSTODIAL -> ImageResource.None
}

fun WalletMode.bottomNavigationItems(): List<ChromeBottomNavigationItem> = when (this) {
    WalletMode.CUSTODIAL -> listOf(
        ChromeBottomNavigationItem.Home,
        ChromeBottomNavigationItem.Prices,
        ChromeBottomNavigationItem.Earn,
    )
    WalletMode.NON_CUSTODIAL -> listOf(
        ChromeBottomNavigationItem.Home,
        ChromeBottomNavigationItem.Prices,
        ChromeBottomNavigationItem.Nft,
        ChromeBottomNavigationItem.Dex,
    )
}
