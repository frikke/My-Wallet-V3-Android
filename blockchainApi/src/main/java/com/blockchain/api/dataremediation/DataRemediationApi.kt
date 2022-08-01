package com.blockchain.api.dataremediation

import com.blockchain.api.adapters.ApiException
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Query

interface DataRemediationApi {

    @GET("kyc/extra-questions")
    suspend fun getQuestionnaire(
        @Header("authorization") authorization: String,
        @Query("context") questionnaireContext: String
    ): Outcome<ApiException, QuestionnaireResponse?>

    @PUT("kyc/extra-questions")
    suspend fun submitQuestionnaire(
        @Header("authorization") authorization: String,
        @Body nodes: QuestionnaireResponse
    ): Outcome<ApiException, Unit>
}
