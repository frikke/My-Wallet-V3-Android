package com.blockchain.api.services

import com.blockchain.api.eligibility.ProductEligibilityApi

class ProductEligibilityService(
    private val api: ProductEligibilityApi
) {

    suspend fun getProductEligibility(authHeader: String) =
        api.getProductEligibility(authHeader)
}
