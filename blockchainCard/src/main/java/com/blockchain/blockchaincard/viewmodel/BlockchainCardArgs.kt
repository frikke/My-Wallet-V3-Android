package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import kotlinx.parcelize.Parcelize

sealed class BlockchainCardArgs : ModelConfigArgs.ParcelableArgs {
    @Parcelize
    data class CardArgs(val card: BlockchainCard) : ModelConfigArgs.ParcelableArgs

    @Parcelize
    data class ProductArgs(val product: BlockchainCardProduct) : ModelConfigArgs.ParcelableArgs
}
