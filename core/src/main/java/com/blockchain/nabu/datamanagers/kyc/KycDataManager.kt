package com.blockchain.nabu.datamanagers.kyc

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.services.KycService
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.nabu.KycQuestionnaireNode
import com.blockchain.nabu.models.responses.nabu.NodeId
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class KycDataManager(
    private val authenticator: Authenticator,
    private val kycService: KycService
) {

    suspend fun getQuestionnaire(): Outcome<KycError, List<KycQuestionnaireNode>> =
        authenticator.getAuthHeader().awaitOutcome()
            .flatMap { authToken -> kycService.getQuestionnaire(authToken) }
            .mapError { KycError.REQUEST_FAILED }
            .map { it?.toDomain() ?: emptyList() }

    suspend fun submitQuestionnaire(nodes: List<KycQuestionnaireNode>): Outcome<SubmitQuestionnaireError, Unit> =
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
