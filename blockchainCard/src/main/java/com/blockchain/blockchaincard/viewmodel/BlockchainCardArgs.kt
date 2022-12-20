package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import kotlinx.parcelize.Parcelize

sealed class BlockchainCardArgs : ModelConfigArgs.ParcelableArgs {
    @Parcelize
    data class CardArgs(
        val cards: List<BlockchainCard>,
        val cardProducts: List<BlockchainCardProduct>,
        val preselectedCard: BlockchainCard? = null,
    ) : ModelConfigArgs.ParcelableArgs

    @Parcelize
    data class ProductArgs(val products: List<BlockchainCardProduct>) : ModelConfigArgs.ParcelableArgs
}
