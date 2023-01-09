package com.blockchain.chrome

import com.blockchain.walletmode.WalletMode

fun WalletMode.backgroundColors(): ChromeBackgroundColors {
    return when (this) {
        WalletMode.CUSTODIAL -> ChromeBackgroundColors.Trading
        WalletMode.NON_CUSTODIAL -> ChromeBackgroundColors.DeFi
    }
}
