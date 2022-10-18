package com.blockchain.chrome

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.walletmode.WalletMode

sealed interface MultiAppIntents : Intent<MultiAppModelState> {
    data class WalletModeChanged(val walletMode: WalletMode) : MultiAppIntents
    object BalanceRevealed : MultiAppIntents
}
