package com.blockchain.api.referral

import com.blockchain.api.adapters.ApiException
import com.blockchain.api.referral.data.ReferralCode
import com.blockchain.api.referral.data.ReferralResponse
import com.blockchain.network.interceptor.AuthenticationNotRequired
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReferralApi {

    @GET("referral/info")
    suspend fun getReferralCode(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("platform") platform: String,
        @Query("currency") currency: String
    ): Outcome<ApiException, ReferralResponse?>

    @AuthenticationNotRequired
    @GET("referral/{code}")
    suspend fun validateReferralCode(
        @Path("code") code: String
    ): Outcome<ApiException, Unit>

    @POST("referral")
    suspend fun associateReferral(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body referralCode: ReferralCode
    ): Outcome<ApiException, Unit>
}
