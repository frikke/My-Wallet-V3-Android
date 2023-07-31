package com.blockchain.componentlib.navigation

import com.blockchain.walletmode.WalletMode

sealed interface ModeBackgroundColor {
    object Current : ModeBackgroundColor
    data class Override(val walletMode: WalletMode) : ModeBackgroundColor
    object None : ModeBackgroundColor
}
