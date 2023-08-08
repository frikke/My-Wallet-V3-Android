package com.blockchain.chrome

import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.blockchain.chrome.common.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Blockchain
import com.blockchain.componentlib.icons.Icons
import com.blockchain.walletmode.WalletMode

fun WalletMode.backgroundColors(): ChromeBackgroundColors {
    return when (this) {
        WalletMode.CUSTODIAL -> ChromeBackgroundColors.Trading
        WalletMode.NON_CUSTODIAL -> ChromeBackgroundColors.DeFi
    }
}

@StringRes
fun WalletMode.titleSuperApp(): Int = when (this) {
    WalletMode.CUSTODIAL -> com.blockchain.stringResources.R.string.brokerage_wallet_name
    WalletMode.NON_CUSTODIAL -> com.blockchain.stringResources.R.string.defi_wallet_name
}

fun WalletMode.titleIcon(): ImageResource.Local? = when (this) {
    WalletMode.CUSTODIAL -> Icons.Filled.Blockchain
    WalletMode.NON_CUSTODIAL -> null
}

val LocalNavControllerProvider = staticCompositionLocalOf<NavHostController> {
    error("No navigation host controller provided.")
}
