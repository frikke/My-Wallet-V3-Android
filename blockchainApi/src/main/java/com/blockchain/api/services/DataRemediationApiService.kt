package com.blockchain.api.services

import com.blockchain.api.adapters.ApiException
import com.blockchain.api.dataremediation.DataRemediationApi
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.outcome.Outcome

class DataRemediationApiService(
    private val api: DataRemediationApi
) {

    suspend fun getQuestionnaire(
        authorization: String,
        questionnaireContext: String
    ): Outcome<ApiException, QuestionnaireResponse?> =
        api.getQuestionnaire(authorization, questionnaireContext)

    suspend fun submitQuestionnaire(
        authorization: String,
        nodes: QuestionnaireResponse
    ): Outcome<ApiException, Unit> =
        api.submitQuestionnaire(authorization, nodes)
}
