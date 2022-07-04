package com.blockchain.core.dataremediation

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.DataRemediationApiService
import com.blockchain.core.dataremediation.mapper.toDomain
import com.blockchain.core.dataremediation.mapper.toError
import com.blockchain.core.dataremediation.mapper.toNetwork
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.DataRemediationError
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class DataRemediationRepository(
    private val authenticator: Authenticator,
    private val api: DataRemediationApiService
) : DataRemediationService {

    override suspend fun getQuestionnaire(
        questionnaireContext: QuestionnaireContext
    ): Outcome<DataRemediationError, Questionnaire?> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { authToken -> api.getQuestionnaire(authToken, questionnaireContext.toNetwork()) }
            .mapError { DataRemediationError.REQUEST_FAILED }
            .map { it?.toDomain() }

    override suspend fun submitQuestionnaire(
        questionnaire: Questionnaire
    ): Outcome<SubmitQuestionnaireError, Unit> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { SubmitQuestionnaireError.RequestFailed(null) }
            .flatMap { authToken ->
                api.submitQuestionnaire(authToken, questionnaire.toNetwork())
                    .mapError(ApiError::toError)
            }
}
