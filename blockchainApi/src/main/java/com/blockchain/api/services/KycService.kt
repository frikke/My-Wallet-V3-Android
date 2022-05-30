package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.kyc.KycApi
import com.blockchain.api.kyc.models.KycQuestionnaireResponse
import com.blockchain.outcome.Outcome

class KycService(
    private val kycApi: KycApi
) {
    suspend fun getQuestionnaire(authorization: String): Outcome<ApiError, KycQuestionnaireResponse?> =
        kycApi.getQuestionnaire(authorization)

    suspend fun submitQuestionnaire(
        authorization: String,
        nodes: KycQuestionnaireResponse
    ): Outcome<ApiError, Unit> =
        kycApi.submitQuestionnaire(authorization, nodes)
}
