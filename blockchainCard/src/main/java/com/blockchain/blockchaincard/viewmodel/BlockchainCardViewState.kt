package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.TradingAccount
import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class BlockchainCardViewState(
    val card: BlockchainCard? = null,
    val cardProduct: BlockchainCardProduct? = null,
    val cardWidgetUrl: String? = null,
    val eligibleTradingAccounts: MutableMap<TradingAccount, AccountBalance?> = mutableMapOf()
) : ViewState
