package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.coincore.AccountBalance
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.domain.eligibility.model.Region

data class BlockchainCardModelState(
    val card: BlockchainCard? = null,
    val selectedCardProduct: BlockchainCardProduct? = null,
    val cardWidgetUrl: String? = null,
    val isLinkedAccountBalanceLoading: Boolean = false,
    val isTransactionListRefreshing: Boolean = false,
    val linkedAccountBalance: AccountBalance? = null,
    var eligibleTradingAccountBalances: List<AccountBalance> = emptyList(),
    val residentialAddress: BlockchainCardAddress? = null,
    val userFirstAndLastName: String? = null,
    val shortTransactionList: List<BlockchainCardTransaction>? = null,
    val pendingTransactions: List<BlockchainCardTransaction>? = null,
    val completedTransactionsGroupedByMonth: Map<String?, List<BlockchainCardTransaction>>? = null,
    val selectedCardTransaction: BlockchainCardTransaction? = null,
    val ssn: String? = null,
    val countryStateList: List<Region.State>? = null,
    val errorState: BlockchainCardErrorState? = null,
    val legalDocuments: List<BlockchainCardLegalDocument>? = null,
    val isLegalDocReviewComplete: Boolean = false,
    val singleLegalDocumentToSee: BlockchainCardLegalDocument? = null,
    val isAddressLoading: Boolean = false,
) : ModelState

sealed class BlockchainCardErrorState {

    abstract val error: BlockchainCardError

    data class SnackbarErrorState(override val error: BlockchainCardError) : BlockchainCardErrorState()
    data class ScreenErrorState(override val error: BlockchainCardError) : BlockchainCardErrorState()
}
