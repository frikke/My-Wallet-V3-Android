package com.blockchain.api.eligibility

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ProductEligibilityApi {

    @GET("products")
    suspend fun getProductEligibility(
        @Header("authorization") authorization: String,
        @Query("product") productType: String = "SIMPLEBUY"
    ): Outcome<ApiError, ProductEligibilityResponse>
}
