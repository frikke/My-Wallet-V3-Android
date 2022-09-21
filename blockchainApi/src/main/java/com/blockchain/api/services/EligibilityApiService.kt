package com.blockchain.api.services

import com.blockchain.api.eligibility.EligibilityApi
import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.outcome.Outcome

class EligibilityApiService(
    private val api: EligibilityApi
) {

    suspend fun getProductEligibility(): Outcome<Exception, ProductEligibilityResponse> =
        api.getProductEligibility()

    suspend fun getCountriesList(scope: String?): Outcome<Exception, List<CountryResponse>> =
        api.getCountriesList(scope)

    suspend fun getStatesList(countryCode: String, scope: String? = null): Outcome<Exception, List<StateResponse>> =
        api.getStatesList(countryCode, scope)
}
