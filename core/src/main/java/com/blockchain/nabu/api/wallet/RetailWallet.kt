package com.blockchain.nabu.api.wallet

import com.blockchain.nabu.models.responses.wallet.RetailJwtResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

internal interface RetailWallet {

    @FormUrlEncoded
    @POST
    fun requestJwt(
        @Url url: String,
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String,
        @Field("api_code") apiCode: String,
        @Field("method") method: String = RETAIL_JWT_TOKEN_METHOD
    ): Single<RetailJwtResponse>
}