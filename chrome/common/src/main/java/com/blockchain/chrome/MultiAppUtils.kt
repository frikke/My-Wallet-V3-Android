package com.blockchain.chrome

import com.blockchain.walletmode.WalletMode

fun WalletMode.backgroundColors(): ChromeBackgroundColors {
    return when (this) {
        WalletMode.CUSTODIAL_ONLY -> ChromeBackgroundColors.Trading
        WalletMode.NON_CUSTODIAL_ONLY -> ChromeBackgroundColors.DeFi
        WalletMode.UNIVERSAL -> error("WalletMode.UNIVERSAL unsupported")
    }
}
