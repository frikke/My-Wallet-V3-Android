package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.eligibility.EligibilityApi
import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.outcome.Outcome

class EligibilityApiService(
    private val api: EligibilityApi
) {

    suspend fun getProductEligibility(authHeader: String): Outcome<ApiError, ProductEligibilityResponse> =
        api.getProductEligibility(authHeader)

    suspend fun getCountriesList(scope: String?): Outcome<ApiError, List<CountryResponse>> =
        api.getCountriesList(scope)

    suspend fun getStatesList(countryCode: String, scope: String?): Outcome<ApiError, List<StateResponse>> =
        api.getStatesList(countryCode, scope)
}
