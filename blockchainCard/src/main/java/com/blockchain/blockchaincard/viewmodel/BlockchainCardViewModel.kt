package com.blockchain.blockchaincard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel

abstract class BlockchainCardViewModel :
    MviViewModel<
        BlockchainCardIntent,
        BlockchainCardViewState,
        BlockchainCardModelState,
        BlockchainCardNavigationEvent,
        ModelConfigArgs>(BlockchainCardModelState())
