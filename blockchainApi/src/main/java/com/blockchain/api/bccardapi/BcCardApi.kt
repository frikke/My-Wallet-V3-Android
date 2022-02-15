package com.blockchain.api.bccardapi

import com.blockchain.api.bccardapi.models.ProductsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface BcCardApi {

    @GET("bc-cards/products")
    fun getProducts(
        @Header("authorization") authorization: String,
    ): Single<List<ProductsResponse>>
}