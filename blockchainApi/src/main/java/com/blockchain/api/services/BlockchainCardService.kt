package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.blockchainCard.BlockchainCardApi
import com.blockchain.api.blockchainCard.data.CardCreationRequestBody
import com.blockchain.api.blockchainCard.data.CardsResponse
import com.blockchain.api.blockchainCard.data.ProductsResponse
import com.blockchain.outcome.Outcome

class BlockchainCardService internal constructor(
    private val api: BlockchainCardApi
) {
    suspend fun getProducts(authHeader: String): Outcome<ApiError, List<ProductsResponse>> =
        api.getProducts(authHeader)

    suspend fun getCards(authHeader: String): Outcome<ApiError, List<CardsResponse>> =
        api.getCards(authHeader)

    suspend fun createCard(
        authHeader: String,
        productCode: String,
        ssn: String
    ): Outcome<ApiError, CardsResponse> = api.createCard(
        authorization = authHeader,
        cardCreationRequest = CardCreationRequestBody(
            productCode = productCode,
            ssn = ssn
        )
    )

    suspend fun deleteCard(
        authHeader: String,
        cardId: String
    ): Outcome<ApiError, CardsResponse> = api.deleteCard(
        authorization = authHeader,
        cardId = cardId
    )
}
