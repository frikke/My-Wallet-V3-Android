package com.blockchain.blockchaincard.data

import com.blockchain.api.bccardapi.models.CardsResponse
import com.blockchain.api.bccardapi.models.ProductsResponse
import com.blockchain.blockchaincard.domain.BcCardApi
import io.reactivex.rxjava3.core.Single

class BcCardService internal constructor(
    private val api: BcCardApi
) {
    fun getProducts(authHeader: String): Single<List<ProductsResponse>> =
        api.getProducts(authHeader)

    fun getCards(authHeader: String): Single<List<CardsResponse>> =
        api.getCards(authHeader)
}
