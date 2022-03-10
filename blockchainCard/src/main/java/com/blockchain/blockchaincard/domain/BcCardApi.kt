package com.blockchain.blockchaincard.domain

import com.blockchain.api.bccardapi.models.CardsResponse
import com.blockchain.api.bccardapi.models.ProductsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header

interface BcCardApi {

    @GET("products")
    fun getProducts(
        @Header("authorization") authorization: String,
    ): Single<List<ProductsResponse>>

    @GET("cards")
    fun getCards(
        @Header("authorization") authorization: String,
    ): Single<List<CardsResponse>>
}
