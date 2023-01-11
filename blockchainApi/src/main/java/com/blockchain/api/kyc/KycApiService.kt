package com.blockchain.api.kyc

import com.blockchain.api.kyc.model.KycFlowResponse
import com.blockchain.api.kyc.model.KycTiersDto
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single

class KycApiService internal constructor(
    private val kycApi: KycApiInterface
) {
    fun getTiers(): Single<KycTiersDto> {
        return kycApi.getTiers()
    }

    suspend fun getKycFlow(): Outcome<Exception, KycFlowResponse?> =
        kycApi.getKycFlow()
}
