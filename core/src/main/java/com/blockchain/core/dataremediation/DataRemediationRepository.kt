package com.blockchain.core.dataremediation

import com.blockchain.api.services.DataRemediationApiService
import com.blockchain.core.dataremediation.mapper.toDomain
import com.blockchain.core.dataremediation.mapper.toError
import com.blockchain.core.dataremediation.mapper.toNetwork
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError

class DataRemediationRepository(
    private val api: DataRemediationApiService
) : DataRemediationService {

    override suspend fun getQuestionnaire(
        questionnaireContext: QuestionnaireContext
    ): Outcome<Exception, Questionnaire?> =
        api.getQuestionnaire(questionnaireContext.toNetwork())
            .map { it?.toDomain() }

    override suspend fun submitQuestionnaire(
        questionnaire: Questionnaire
    ): Outcome<SubmitQuestionnaireError, Unit> =
        api.submitQuestionnaire(questionnaire.toNetwork())
            .mapError(Exception::toError)
}
