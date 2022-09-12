package com.blockchain.api.eligibility

import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.network.interceptor.AuthenticationNotRequired
import com.blockchain.network.interceptor.Cacheable
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EligibilityApi {

    @GET("products")
    suspend fun getProductEligibility(
        @Query("product") productType: String = "SIMPLEBUY"
    ): Outcome<Exception, ProductEligibilityResponse>

    @AuthenticationNotRequired
    @GET("countries")
    suspend fun getCountriesList(
        @Query("scope") scope: String?
    ): Outcome<Exception, List<CountryResponse>>

    @AuthenticationNotRequired
    @Cacheable(maxAge = Cacheable.MAX_AGE_1_DAY)
    @GET("countries/{countryCode}/states")
    suspend fun getStatesList(
        @Path("countryCode") countryCode: String,
        @Query("scope") scope: String?
    ): Outcome<Exception, List<StateResponse>>
}
