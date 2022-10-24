package com.blockchain.blockchaincard.viewmodel

import androidx.compose.runtime.mutableStateListOf
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardOrderState
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatement
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.coincore.AccountBalance
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.domain.eligibility.model.Region

data class BlockchainCardViewState(
    val currentCard: BlockchainCard? = null,
    val defaultCardId: String = "",
    val cardList: List<BlockchainCard>? = emptyList(),
    val selectedCardProduct: BlockchainCardProduct? = null,
    val cardWidgetUrl: String? = null,
    val isLinkedAccountBalanceLoading: Boolean = false,
    val isTransactionListRefreshing: Boolean = false,
    val linkedAccountBalance: AccountBalance? = null,
    val eligibleTradingAccountBalances: List<AccountBalance> = mutableStateListOf(),
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
    val isAddressLoading: Boolean = true,
    val googleWalletStatus: BlockchainCardGoogleWalletStatus = BlockchainCardGoogleWalletStatus.NOT_ADDED,
    val cardOrderState: BlockchainCardOrderState? = null,
    val cardActivationUrl: String? = null,
    val cardStatements: List<BlockchainCardStatement>? = null,
) : ViewState
