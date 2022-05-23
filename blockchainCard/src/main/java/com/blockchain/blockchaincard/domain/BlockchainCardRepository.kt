package com.blockchain.blockchaincard.domain

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAccount
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.outcome.Outcome

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

    suspend fun getCardWidgetToken(
        cardId: String
    ): Outcome<BlockchainCardError, String>

    suspend fun getCardWidgetUrl(
        cardId: String,
        last4Digits: String
    ): Outcome<BlockchainCardError, String>

    suspend fun getEligibleTradingAccounts(
        cardId: String
    ): Outcome<BlockchainCardError, List<TradingAccount>>

    suspend fun getCardLinkedAccounts(
        authHeader: String,
        cardId: String
    ): Outcome<BlockchainCardError, List<BlockchainCardAccount>>

    suspend fun linkCardAccount(
        authHeader: String,
        cardId: String,
        accountCurrency: String
    ): Outcome<BlockchainCardError, BlockchainCardAccount>

    suspend fun loadAccountBalance(tradingAccount: BlockchainAccount): Outcome<BlockchainCardError, AccountBalance>
}
