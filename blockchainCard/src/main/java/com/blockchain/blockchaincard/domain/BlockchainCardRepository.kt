package com.blockchain.blockchaincard.domain

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletData
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletPushTokenizeData
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardOrderState
import com.blockchain.blockchaincard.domain.models.BlockchainCardPostMessageType
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatement
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo

interface BlockchainCardRepository {
    suspend fun getProducts(): Outcome<BlockchainCardError, List<BlockchainCardProduct>>

    suspend fun getCards(): Outcome<BlockchainCardError, List<BlockchainCard>>

    suspend fun createCard(
        productCode: String,
        ssn: String
    ): Outcome<BlockchainCardError, BlockchainCard>

    suspend fun getCard(
        cardId: String
    ): Outcome<BlockchainCardError, BlockchainCard>

    suspend fun deleteCard(
        cardId: String
    ): Outcome<BlockchainCardError, BlockchainCard>

    suspend fun lockCard(
        cardId: String
    ): Outcome<BlockchainCardError, BlockchainCard>

    suspend fun unlockCard(
        cardId: String
    ): Outcome<BlockchainCardError, BlockchainCard>

    suspend fun getCardWidgetUrl(
        cardId: String,
        last4Digits: String,
        userFullName: String
    ): Outcome<BlockchainCardError, String>

    suspend fun getEligibleTradingAccounts(
        cardId: String
    ): Outcome<BlockchainCardError, List<TradingAccount>>

    suspend fun linkCardAccount(
        cardId: String,
        accountCurrency: String
    ): Outcome<BlockchainCardError, String>

    suspend fun getCardLinkedAccount(
        cardId: String
    ): Outcome<BlockchainCardError, TradingAccount>

    suspend fun loadAccountBalance(
        tradingAccount: BlockchainAccount
    ): Outcome<BlockchainCardError, AccountBalance>

    suspend fun getAsset(
        networkTicker: String
    ): Outcome<BlockchainCardError, AssetInfo>

    suspend fun getFiatAccount(
        networkTicker: String
    ): Outcome<BlockchainCardError, FiatAccount>

    suspend fun getResidentialAddress(): Outcome<BlockchainCardError, BlockchainCardAddress>

    suspend fun updateResidentialAddress(
        address: BlockchainCardAddress
    ): Outcome<BlockchainCardError, BlockchainCardAddress>

    suspend fun getUserFirstAndLastName(): Outcome<BlockchainCardError, String>

    suspend fun getTransactions(
        limit: Int? = null,
        toId: String? = null
    ): Outcome<BlockchainCardError, List<BlockchainCardTransaction>>

    suspend fun getStatesList(countryCode: String): Outcome<BlockchainCardError, List<Region.State>>

    suspend fun getLegalDocuments(): Outcome<BlockchainCardError, List<BlockchainCardLegalDocument>>

    suspend fun acceptLegalDocuments(
        acceptedLegalDocuments: List<BlockchainCardLegalDocument>
    ): Outcome<BlockchainCardError, List<BlockchainCardLegalDocument>>

    suspend fun provisionGoogleWalletCard(
        cardId: String,
        provisionRequest: BlockchainCardGoogleWalletData
    ): Outcome<BlockchainCardError, BlockchainCardGoogleWalletPushTokenizeData>

    suspend fun getGoogleWalletId(): Outcome<BlockchainCardError, String>
    suspend fun getGoogleWalletStableHardwareId(): Outcome<BlockchainCardError, String>
    suspend fun getGoogleWalletTokenizationStatus(last4Digits: String): Outcome<BlockchainCardError, Boolean>

    fun getDefaultCard(): String
    fun saveCardAsDefault(cardId: String)

    suspend fun getCardOrderState(cardId: String): Outcome<BlockchainCardError, BlockchainCardOrderState>

    suspend fun getCardActivationUrl(): Outcome<BlockchainCardError, String>

    suspend fun getCardStatements(): Outcome<BlockchainCardError, List<BlockchainCardStatement>>

    suspend fun getCardStatementUrl(statementId: String): Outcome<BlockchainCardError, String>

    suspend fun decodePostMessageType(postMessage: String): Outcome<BlockchainCardError, BlockchainCardPostMessageType>
}
