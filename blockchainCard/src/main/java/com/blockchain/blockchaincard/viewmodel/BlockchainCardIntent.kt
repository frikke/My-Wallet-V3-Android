package com.blockchain.blockchaincard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class BlockchainCardIntent : Intent<BlockchainCardModelState> {
    object OrderCard : BlockchainCardIntent()
    object LinkCard : BlockchainCardIntent()
    object ManageCard : BlockchainCardIntent()
}