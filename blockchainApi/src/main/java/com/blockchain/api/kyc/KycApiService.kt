package com.blockchain.api.kyc

import com.blockchain.api.kyc.model.KycTiersDto
import io.reactivex.rxjava3.core.Single

class KycApiService internal constructor(
    private val kycApi: KycApiInterface
) {
    fun getTiers(authHeader: String): Single<KycTiersDto> {
        return kycApi.getTiers(authorization = authHeader)
    }
}
