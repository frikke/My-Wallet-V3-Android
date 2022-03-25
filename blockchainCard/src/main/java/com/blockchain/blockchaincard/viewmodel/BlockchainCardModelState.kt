package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.data.BlockchainDebitCardProduct
import com.blockchain.commonarch.presentation.mvi_v2.ModelState

sealed class BlockchainCardModelState : ModelState {
    data class NotOrdered(val cardProduct: BlockchainDebitCardProduct) : BlockchainCardModelState()
    object OrderCard : BlockchainCardModelState()
    object LinkCard : BlockchainCardModelState()
    object CardCreationSuccess: BlockchainCardModelState()
    object CardCreationFailed: BlockchainCardModelState()
    data class Created(val cardId: String) : BlockchainCardModelState()
    object Unknown : BlockchainCardModelState()
    data class ShowProductDetails(val product: BlockchainDebitCardProduct): BlockchainCardModelState()
}
