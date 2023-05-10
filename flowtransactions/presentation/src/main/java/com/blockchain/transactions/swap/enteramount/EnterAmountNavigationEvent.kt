package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface EnterAmountNavigationEvent : NavigationEvent {
    object Preview : EnterAmountNavigationEvent
}
