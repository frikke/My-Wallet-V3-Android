package com.blockchain.api.kyc

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.kyc.models.KycQuestionnaireResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface KycApi {

    @GET("kyc/extra-questions")
    suspend fun getQuestionnaire(
        @Header("authorization") authorization: String
    ): Outcome<ApiError, KycQuestionnaireResponse?>

    @PUT("kyc/extra-questions")
    suspend fun submitQuestionnaire(
        @Header("authorization") authorization: String,
        @Body nodes: KycQuestionnaireResponse
    ): Outcome<ApiError, Unit>
}
