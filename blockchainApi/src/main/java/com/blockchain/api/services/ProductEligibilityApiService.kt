package com.blockchain.api.services

import com.blockchain.api.eligibility.ProductEligibilityApi

class ProductEligibilityApiService(
    private val api: ProductEligibilityApi
) {

    suspend fun getProductEligibility(authHeader: String) =
        api.getProductEligibility(authHeader)
}
