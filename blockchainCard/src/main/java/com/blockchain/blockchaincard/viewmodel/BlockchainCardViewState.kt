package com.blockchain.blockchaincard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

sealed class BlockchainCardViewState : ViewState {
    object OrderOrLinkCard : BlockchainCardViewState()
    object OrderCard : BlockchainCardViewState()
    object LinkCard : BlockchainCardViewState()
    data class ManageCard(val card: String): BlockchainCardViewState()
}