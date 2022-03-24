package com.blockchain.api.kyc

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.kyc.models.KycAdditionalInfoResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface KycApi {

    @GET("kyc/extra-questions")
    suspend fun getAdditionalInfoForm(
        @Header("authorization") authorization: String
    ): Outcome<ApiError, KycAdditionalInfoResponse?>

    @PUT("kyc/extra-questions")
    suspend fun updateAdditionalInfo(
        @Header("authorization") authorization: String,
        @Body nodes: KycAdditionalInfoResponse
    ): Outcome<ApiError, Unit>
}
