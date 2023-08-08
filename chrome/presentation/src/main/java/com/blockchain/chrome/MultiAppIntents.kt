package com.blockchain.chrome

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.walletmode.WalletMode

sealed interface MultiAppIntents : Intent<MultiAppModelState> {
    object LoadData : MultiAppIntents
    data class WalletModeSelected(val walletMode: WalletMode) : MultiAppIntents
    object BalanceRevealed : MultiAppIntents
    data class BottomNavigationItemSelected(val item: ChromeBottomNavigationItem) : MultiAppIntents {
        override fun isValidFor(modelState: MultiAppModelState): Boolean {
            return modelState.selectedBottomNavigationItem != item
        }
    }
}
