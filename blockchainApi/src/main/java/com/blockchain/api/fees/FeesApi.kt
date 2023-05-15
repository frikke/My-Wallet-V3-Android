package com.blockchain.api.fees

import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Query

internal interface FeesApi {
    @GET("withdrawals/fees")
    fun withdrawalFessAndMinAmount(
        @Query("product") product: String,
        @Query("paymentMethod") paymentMethod: String,
        @Query("currency") currency: String,
        @Query("fiatCurrency") fiatCurrency: String,
        @Query("amount") amount: String,
        @Query("max") max: Boolean
    ): Single<WithdrawFeesAndMinLimitResponse>
}
