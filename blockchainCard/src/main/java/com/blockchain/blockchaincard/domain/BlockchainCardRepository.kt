package com.blockchain.blockchaincard.domain

import com.blockchain.blockchaincard.domain.models.BlockchainDebitCard
import com.blockchain.blockchaincard.domain.models.BlockchainDebitCardProduct
import io.reactivex.rxjava3.core.Single

interface BlockchainCardRepository {
    fun getProducts(): Single<List<BlockchainDebitCardProduct>>

    fun getCards(): Single<List<BlockchainDebitCard>>

    fun createCard(productCode: String, ssn: String): Single<BlockchainDebitCard>

    fun deleteCard(cardId: String): Single<BlockchainDebitCard>
}
