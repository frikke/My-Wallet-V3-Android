package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardOrderState
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatement
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.coincore.AccountBalance
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.domain.eligibility.model.Region

data class BlockchainCardModelState(
    val currentCard: BlockchainCard? = null,
    val defaultCardId: String = "",
    val cardList: List<BlockchainCard>? = emptyList(),
    val cardProductList: List<BlockchainCardProduct>? = emptyList(),
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
    val nextPageId: String? = null,
    val selectedCardTransaction: BlockchainCardTransaction? = null,
    val ssn: String? = null,
    val countryStateList: List<Region.State>? = null,
    val errorState: BlockchainCardErrorState? = null,
    val legalDocuments: List<BlockchainCardLegalDocument>? = null,
    val isLegalDocReviewComplete: Boolean = false,
    val singleLegalDocumentToSee: BlockchainCardLegalDocument? = null,
    val isAddressLoading: Boolean = false,
    val googleWalletId: String? = null,
    val stableHardwareId: String? = null,
    val googleWalletStatus: BlockchainCardGoogleWalletStatus = BlockchainCardGoogleWalletStatus.NOT_ADDED,
    val cardOrderState: BlockchainCardOrderState? = null,
    val cardActivationUrl: String? = null,
    val cardStatements: List<BlockchainCardStatement>? = null,
    val shippingAddress: BlockchainCardAddress? = null,
) : ModelState

sealed class BlockchainCardErrorState {

    abstract val error: BlockchainCardError

    data class SnackbarErrorState(override val error: BlockchainCardError) : BlockchainCardErrorState()
    data class ScreenErrorState(override val error: BlockchainCardError) : BlockchainCardErrorState()
}
