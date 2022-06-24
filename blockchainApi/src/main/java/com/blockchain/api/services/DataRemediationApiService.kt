package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.dataremediation.DataRemediationApi
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.outcome.Outcome

class DataRemediationApiService(
    private val api: DataRemediationApi
) {
    suspend fun getQuestionnaire(authorization: String): Outcome<ApiError, QuestionnaireResponse?> =
        api.getQuestionnaire(authorization)

    suspend fun submitQuestionnaire(
        authorization: String,
        nodes: QuestionnaireResponse
    ): Outcome<ApiError, Unit> =
        api.submitQuestionnaire(authorization, nodes)
}
