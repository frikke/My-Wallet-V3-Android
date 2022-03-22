package com.blockchain.api.services

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.kyc.KycApi
import com.blockchain.api.kyc.models.KycAdditionalInfoResponse
import com.blockchain.outcome.Outcome

class KycService(
    private val kycApi: KycApi
) {
    suspend fun getAdditionalInfoForm(authorization: String): Outcome<ApiError, KycAdditionalInfoResponse> =
        kycApi.getAdditionalInfoForm(authorization)

    suspend fun updateAdditionalInfo(
        authorization: String,
        nodes: KycAdditionalInfoResponse
    ): Outcome<ApiError, Unit> =
        kycApi.updateAdditionalInfo(authorization, nodes)
}
