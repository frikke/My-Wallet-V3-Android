package com.blockchain.api.eligibility

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.eligibility.data.CountryResponse
import com.blockchain.api.eligibility.data.ProductEligibilityResponse
import com.blockchain.api.eligibility.data.StateResponse
import com.blockchain.network.interceptor.Cacheable
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface EligibilityApi {

    @GET("products")
    suspend fun getProductEligibility(
        @Header("authorization") authorization: String,
        @Query("product") productType: String = "SIMPLEBUY"
    ): Outcome<ApiError, ProductEligibilityResponse>

    @GET("countries")
    suspend fun getCountriesList(
        @Query("scope") scope: String?
    ): Outcome<ApiError, List<CountryResponse>>

    @Cacheable(maxAge = Cacheable.MAX_AGE_1_DAY)
    @GET("countries/{countryCode}/states")
    suspend fun getStatesList(
        @Path("countryCode") countryCode: String,
        @Query("scope") scope: String?
    ): Outcome<ApiError, List<StateResponse>>
}
