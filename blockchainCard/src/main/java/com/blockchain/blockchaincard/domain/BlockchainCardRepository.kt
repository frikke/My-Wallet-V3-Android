package com.blockchain.blockchaincard.domain

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo

interface BlockchainCardRepository {
    suspend fun getProducts(): Outcome<BlockchainCardError, List<BlockchainCardProduct>>

    suspend fun getCards(): Outcome<BlockchainCardError, List<BlockchainCard>>

    suspend fun createCard(
        productCode: String,
        ssn: String
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
        last4Digits: String
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

    suspend fun getTransactions(): Outcome<BlockchainCardError, List<BlockchainCardTransaction>>
}
