package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.transactions.swap.confirmation.composable.ConfirmationArgs

sealed interface EnterAmountNavigationEvent : NavigationEvent {
    data class Preview(
        val data: ConfirmationArgs,
    ) : EnterAmountNavigationEvent
}
