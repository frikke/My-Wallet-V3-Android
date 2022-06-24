package com.blockchain.core.dataremediation

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.DataRemediationApiService
import com.blockchain.core.dataremediation.mapper.toDomain
import com.blockchain.core.dataremediation.mapper.toNetwork
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.DataRemediationError
import com.blockchain.domain.dataremediation.model.NodeId
import com.blockchain.domain.dataremediation.model.QuestionnaireNode
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError
import com.blockchain.nabu.Authenticator
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class DataRemediationRepository(
    private val authenticator: Authenticator,
    private val kycService: DataRemediationApiService
) : DataRemediationService {

    override suspend fun getQuestionnaire(): Outcome<DataRemediationError, List<QuestionnaireNode>> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { authToken -> kycService.getQuestionnaire(authToken) }
            .mapError { DataRemediationError.REQUEST_FAILED }
            .map { it?.toDomain() ?: emptyList() }

    override suspend fun submitQuestionnaire(nodes: List<QuestionnaireNode>): Outcome<SubmitQuestionnaireError, Unit> =
        authenticator.getAuthHeader().awaitOutcome()
            .mapError { SubmitQuestionnaireError.RequestFailed }
            .flatMap { authToken ->
                kycService.submitQuestionnaire(authToken, nodes.toNetwork())
                    .mapError {
                        val nodeId = it.tryParseNodeIdFromApiError()
                        if (nodeId != null) {
                            SubmitQuestionnaireError.InvalidNode(nodeId)
                        } else {
                            SubmitQuestionnaireError.RequestFailed
                        }
                    }
            }

    private fun ApiError.tryParseNodeIdFromApiError(): NodeId? =
        if (this is ApiError.KnownError && errorDescription.count { it == '#' } == 2) {
            val indexOfStart = errorDescription.indexOf('#') + 1
            val indexOfEnd = errorDescription.substring(indexOfStart).indexOf('#')
            errorDescription.substring(indexOfStart, indexOfStart + indexOfEnd)
        } else {
            null
        }
}
