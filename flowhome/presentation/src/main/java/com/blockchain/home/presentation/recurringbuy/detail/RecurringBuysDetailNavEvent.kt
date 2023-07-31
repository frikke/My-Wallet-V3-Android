package com.blockchain.home.presentation.recurringbuy.detail

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface RecurringBuysDetailNavEvent : NavigationEvent {
    object Close : RecurringBuysDetailNavEvent
}
