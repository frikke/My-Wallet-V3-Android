package com.blockchain.chrome.composable

import androidx.annotation.StringRes
import com.blockchain.chrome.ChromeBottomNavigationItem
import com.blockchain.chrome.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.walletmode.WalletMode
import java.util.concurrent.TimeUnit

const val ANIMATION_DURATION = 400
val REVEAL_BALANCE_DELAY_MS = TimeUnit.SECONDS.toMillis(3)

@StringRes
fun WalletMode.titleSuperApp(): Int = when (this) {
    WalletMode.CUSTODIAL_ONLY -> R.string.brokerage_wallet_name_superapp
    WalletMode.NON_CUSTODIAL_ONLY -> R.string.defi_wallet_name_superapp
    else -> error("UNIVERSAL not supported")
}

fun WalletMode.titleIcon(): ImageResource = when (this) {
    WalletMode.CUSTODIAL_ONLY -> ImageResource.Local(R.drawable.ic_brokerage_logo)
    WalletMode.NON_CUSTODIAL_ONLY -> ImageResource.None
    else -> error("UNIVERSAL not supported")
}

fun WalletMode.bottomNavigationItems(): List<ChromeBottomNavigationItem> = when (this) {
    WalletMode.CUSTODIAL_ONLY -> listOf(
        ChromeBottomNavigationItem.Home,
        ChromeBottomNavigationItem.Prices,
    )
    WalletMode.NON_CUSTODIAL_ONLY -> listOf(
        ChromeBottomNavigationItem.Home,
        ChromeBottomNavigationItem.Prices,
    )
    else -> error("UNIVERSAL not supported")
}
