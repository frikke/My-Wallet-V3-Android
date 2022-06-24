package com.blockchain.api.dataremediation

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface DataRemediationApi {

    @GET("kyc/extra-questions")
    suspend fun getQuestionnaire(
        @Header("authorization") authorization: String
    ): Outcome<ApiError, QuestionnaireResponse?>

    @PUT("kyc/extra-questions")
    suspend fun submitQuestionnaire(
        @Header("authorization") authorization: String,
        @Body nodes: QuestionnaireResponse
    ): Outcome<ApiError, Unit>
}
