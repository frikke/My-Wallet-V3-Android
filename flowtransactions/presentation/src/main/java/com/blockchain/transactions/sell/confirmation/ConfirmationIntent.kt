package com.blockchain.transactions.sell.confirmation

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface ConfirmationIntent : Intent<ConfirmationModelState> {
    object SubmitClicked : ConfirmationIntent
}
