package com.blockchain.blockchaincard.domain

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.outcome.Outcome

interface BlockchainCardRepository {
    suspend fun getProducts(): Outcome<BlockchainCardError, List<BlockchainCardProduct>>

    suspend fun getCards(): Outcome<BlockchainCardError, List<BlockchainCard>>

    suspend fun createCard(productCode: String, ssn: String): Outcome<BlockchainCardError, BlockchainCard>

    suspend fun deleteCard(cardId: String): Outcome<BlockchainCardError, BlockchainCard>

    suspend fun getCardWidgetToken(cardId: String): Outcome<BlockchainCardError, String>

    suspend fun getCardWidgetUrl(cardId: String, last4Digits: String): Outcome<BlockchainCardError, String>
}
