package com.blockchain.api.services

import com.blockchain.api.adapters.ApiException
import com.blockchain.api.eligibility.EligibilityApi
import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.outcome.Outcome

class EligibilityApiService(
    private val api: EligibilityApi
) {

    suspend fun getProductEligibility(authHeader: String): Outcome<ApiException, ProductEligibilityResponse> =
        api.getProductEligibility(authHeader)

    suspend fun getCountriesList(scope: String?): Outcome<ApiException, List<CountryResponse>> =
        api.getCountriesList(scope)

    suspend fun getStatesList(countryCode: String, scope: String? = null): Outcome<ApiException, List<StateResponse>> =
        api.getStatesList(countryCode, scope)
}
