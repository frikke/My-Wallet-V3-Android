package com.blockchain.blockchaincard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

sealed class BlockchainCardModelState : ModelState {
    object NotOrdered : BlockchainCardModelState()
    object OrderCard : BlockchainCardModelState()
    object LinkCard : BlockchainCardModelState()
    data class Created(val card: String) : BlockchainCardModelState()
    object Unknown : BlockchainCardModelState()
}
