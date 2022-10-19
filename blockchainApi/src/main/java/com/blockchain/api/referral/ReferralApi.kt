package com.blockchain.api.referral

import com.blockchain.api.referral.data.ReferralCode
import com.blockchain.api.referral.data.ReferralResponse
import com.blockchain.network.interceptor.AuthenticationNotRequired
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReferralApi {

    @GET("referral/info")
    suspend fun getReferralCode(
        @Query("platform") platform: String,
        @Query("currency") currency: String
    ): Outcome<Exception, ReferralResponse?>

    @AuthenticationNotRequired
    @GET("referral/{code}")
    suspend fun validateReferralCode(
        @Path("code") code: String,
        @Query("platform") platform: String
    ): Outcome<Exception, Unit>

    @POST("referral")
    suspend fun associateReferral(
        @Body referralCode: ReferralCode
    ): Outcome<Exception, Unit>
}
