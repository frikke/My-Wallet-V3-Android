package com.blockchain.transactions.receive.detail

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface ReceiveAccountDetailIntent : Intent<ReceiveAccountDetailModelState> {
    object LoadData : ReceiveAccountDetailIntent
}
