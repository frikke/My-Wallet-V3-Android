package com.blockchain.api.services

import com.blockchain.api.dataremediation.DataRemediationApi
import com.blockchain.api.dataremediation.models.QuestionnaireResponse
import com.blockchain.outcome.Outcome

class DataRemediationApiService(
    private val api: DataRemediationApi
) {

    suspend fun getQuestionnaire(
        questionnaireContext: String
    ): Outcome<Exception, QuestionnaireResponse?> =
        api.getQuestionnaire(questionnaireContext)

    suspend fun submitQuestionnaire(
        nodes: QuestionnaireResponse
    ): Outcome<Exception, Unit> =
        api.submitQuestionnaire(nodes)
}
