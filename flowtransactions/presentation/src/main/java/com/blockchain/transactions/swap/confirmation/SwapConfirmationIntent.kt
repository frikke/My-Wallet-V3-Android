package com.blockchain.transactions.swap.confirmation

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface SwapConfirmationIntent : Intent<SwapConfirmationModelState> {
    object SubmitClicked : SwapConfirmationIntent
}
