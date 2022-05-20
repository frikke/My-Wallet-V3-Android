package com.blockchain.api.referral

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.referral.data.ReferralResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ReferralApi {

    @GET("referral")
    suspend fun getReferralCode(
        @Header("authorization") authorization: String,
        @Query("platform") platform: String,
        @Query("currency") currency: String
    ): Outcome<ApiError, ReferralResponse>
}
