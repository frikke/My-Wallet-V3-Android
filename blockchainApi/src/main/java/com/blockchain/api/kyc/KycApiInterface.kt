package com.blockchain.api.kyc

import com.blockchain.api.kyc.model.KycTiersDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header

internal interface KycApiInterface {
    @GET("kyc/tiers")
    fun getTiers(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<KycTiersDto>
}
