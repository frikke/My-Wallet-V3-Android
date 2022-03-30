package com.blockchain.blockchaincard.domain

import com.blockchain.api.blockchainCard.api.BlockchainCardApi
import com.blockchain.api.blockchainCard.api.CardCreationRequestBody
import com.blockchain.api.blockchainCard.api.CardsResponse
import com.blockchain.api.blockchainCard.api.ProductsResponse
import io.reactivex.rxjava3.core.Single

class BlockchainCardService internal constructor(
    private val api: BlockchainCardApi
) {
    fun getProducts(authHeader: String): Single<List<ProductsResponse>> =
        api.getProducts(authHeader)

    fun getCards(authHeader: String): Single<List<CardsResponse>> =
        api.getCards(authHeader)

    fun createCard(
        authHeader: String,
        productCode: String,
        ssn: String
    ): Single<CardsResponse> = api.createCard(
        authorization = authHeader,
        cardCreationRequest = CardCreationRequestBody(
            productCode = productCode,
            ssn = ssn
        )
    )

    fun deleteCard(
        authHeader: String,
        cardId: String
    ): Single<CardsResponse> = api.deleteCard(
        authorization = authHeader,
        cardId = cardId
    )
}
