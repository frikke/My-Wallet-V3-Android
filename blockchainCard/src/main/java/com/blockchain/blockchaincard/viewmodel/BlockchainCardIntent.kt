package com.blockchain.blockchaincard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class BlockchainCardIntent : Intent<BlockchainCardModelState> {
    object OrderCard : BlockchainCardIntent()
    object LinkCard : BlockchainCardIntent()
    data class CreateCard(val productCode: String, val ssn: String) : BlockchainCardIntent()
    object OnSeeProductDetails : BlockchainCardIntent()
    object ManageCard : BlockchainCardIntent()
    object HideBottomSheet : BlockchainCardIntent()
}