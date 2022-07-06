package com.blockchain.blockchaincard.viewmodel

import androidx.compose.runtime.mutableStateListOf
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.coincore.AccountBalance
import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class BlockchainCardViewState(
    val card: BlockchainCard? = null,
    val selectedCardProduct: BlockchainCardProduct? = null,
    val cardWidgetUrl: String? = null,
    val isLinkedAccountBalanceLoading: Boolean = false,
    val isTransactionListRefreshing: Boolean = false,
    val linkedAccountBalance: AccountBalance? = null,
    val eligibleTradingAccountBalances: MutableList<AccountBalance> = mutableStateListOf(),
    val residentialAddress: BlockchainCardAddress? = null,
    val userFirstAndLastName: String? = null,
    val transactionList: List<BlockchainCardTransaction>? = null,
    val selectedCardTransaction: BlockchainCardTransaction? = null,
    val ssn: String? = null,
) : ViewState
