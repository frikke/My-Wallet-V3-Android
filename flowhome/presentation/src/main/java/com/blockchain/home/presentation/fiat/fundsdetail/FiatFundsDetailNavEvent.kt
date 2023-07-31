package com.blockchain.home.presentation.fiat.fundsdetail

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface FiatFundsDetailNavEvent : NavigationEvent {
    object Dismss : FiatFundsDetailNavEvent
}
