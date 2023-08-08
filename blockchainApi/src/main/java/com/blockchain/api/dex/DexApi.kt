package com.blockchain.api.dex

import com.blockchain.network.interceptor.Cacheable
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Query

interface DexApi {
    @GET("chains")
    suspend fun chains(): Outcome<Exception, List<DexChainResponse>>

    @GET("venues")
    suspend fun venues(): Outcome<Exception, List<DexVenueResponse>>

    @GET("tokens")
    suspend fun tokens(
        @Query("chainId") chainId: Int,
        @Query("queryBy") queryBy: String,
        @Query("limit") limit: Int = Int.MAX_VALUE
    ): Outcome<Exception, List<DexTokenResponse>>

    @Cacheable(maxAge = Cacheable.MAX_AGE_THREE_DAYS)
    @GET("dex/eligible")
    suspend fun eligibility(
        @Query("product") product: String,
        @Query("walletAddress") walletAddress: String,
    ): Outcome<Exception, DexEligibilityResponse>
}
