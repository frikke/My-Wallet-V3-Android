package com.blockchain.api.kyc

import com.blockchain.api.kyc.model.FinishMobileAuthResponse
import com.blockchain.api.kyc.model.PossessionStateResponse
import com.blockchain.api.kyc.model.PrefillDataResponse
import com.blockchain.api.kyc.model.PrefillDataSubmissionRequest
import com.blockchain.api.kyc.model.StartInstantLinkAuthResponse
import com.blockchain.api.kyc.model.StartMobileAuthResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ProveApi {

    @POST("kyc/prove/auth/instant-link/start")
    suspend fun startInstantLinkAuth(
        @Query("mobileNumber") mobileNumber: String,
    ): Outcome<Exception, StartInstantLinkAuthResponse>

    @POST("kyc/prove/auth/mobile/start")
    suspend fun startMobileAuth(): Outcome<Exception, StartMobileAuthResponse>

    @POST("kyc/prove/auth/mobile/finish")
    suspend fun finishMobileAuth(): Outcome<Exception, FinishMobileAuthResponse>

    @GET("kyc/prove/auth/status")
    suspend fun getPossessionState(): Outcome<Exception, PossessionStateResponse>

    @POST("kyc/prove/pre-fill")
    suspend fun getPrefillData(
        @Query("dob") dob: String, // ISO 8601
    ): Outcome<Exception, PrefillDataResponse>

    @POST("kyc/prove/pii")
    suspend fun submitData(
        @Body data: PrefillDataSubmissionRequest,
    ): Outcome<Exception, Unit>
}
