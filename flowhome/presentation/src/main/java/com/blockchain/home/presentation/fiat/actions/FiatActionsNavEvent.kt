package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface FiatActionsNavEvent : NavigationEvent {
    data class WireTransferAccountDetails(val account: FiatAccount) : FiatActionsNavEvent
}
