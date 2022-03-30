package com.blockchain.blockchaincard.data

import com.blockchain.blockchaincard.domain.BcCardApi
import com.blockchain.blockchaincard.domain.CardCreationRequestBody
import com.blockchain.blockchaincard.domain.CardsResponse
import com.blockchain.blockchaincard.domain.ProductsResponse
import io.reactivex.rxjava3.core.Single

class BcCardService internal constructor(
    private val api: BcCardApi
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
