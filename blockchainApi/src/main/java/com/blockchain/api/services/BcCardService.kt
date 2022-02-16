package com.blockchain.api.services

import com.blockchain.api.bccardapi.BcCardApi
import com.blockchain.api.bccardapi.models.ProductsResponse
import io.reactivex.rxjava3.core.Single

class BcCardService internal constructor(
    private val api: BcCardApi
) {
    fun getProducts(authHeader: String): Single<List<ProductsResponse>> =
        api.getProducts(authHeader)
}