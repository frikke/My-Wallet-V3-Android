package com.blockchain.api.services

import com.blockchain.api.dataremediation.DataRemediationApi
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.outcome.Outcome

class DataRemediationApiService(
    private val api: DataRemediationApi
) {

    suspend fun getQuestionnaire(
        authorization: String,
        questionnaireContext: String
    ): Outcome<Exception, QuestionnaireResponse?> =
        api.getQuestionnaire(authorization, questionnaireContext)

    suspend fun submitQuestionnaire(
        authorization: String,
        nodes: QuestionnaireResponse
    ): Outcome<Exception, Unit> =
        api.submitQuestionnaire(authorization, nodes)
}
