package com.blockchain.api.eligibility

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Header

interface ProductEligibilityApi {

    @GET("products")
    suspend fun getProductEligibility(
        @Header("authorization") authorization: String,
    ): Outcome<ApiError, ProductEligibilityResponse>
}
