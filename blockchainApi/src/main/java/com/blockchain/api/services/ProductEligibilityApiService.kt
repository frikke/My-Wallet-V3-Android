package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.eligibility.ProductEligibilityApi
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.outcome.Outcome

class ProductEligibilityApiService(
    private val api: ProductEligibilityApi
) {

    suspend fun getProductEligibility(authHeader: String): Outcome<ApiError, ProductEligibilityResponse> =
        api.getProductEligibility(authHeader)
}
