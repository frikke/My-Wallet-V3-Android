package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.models.BlockchainDebitCardProduct
import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class BlockchainCardModelState(
    val cardId: String? = null,
    val cardProduct: BlockchainDebitCardProduct? = null
) : ModelState
