package com.blockchain.api.kyc

import com.blockchain.api.kyc.model.KycFlowResponse
import com.blockchain.api.kyc.model.KycTiersDto
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

internal interface KycApiInterface {
    @GET("kyc/tiers")
    fun getTiers(): Single<KycTiersDto>

    @GET("flows/kyc")
    suspend fun getKycFlow(): Outcome<Exception, KycFlowResponse?>
}
