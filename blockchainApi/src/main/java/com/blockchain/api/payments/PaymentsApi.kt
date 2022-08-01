package com.blockchain.api.payments

import com.blockchain.api.payments.data.PaymentMethodDetailsResponse
import com.blockchain.api.payments.data.WithdrawalLocksResponse
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PaymentsApi {

    @GET("payments/beneficiaries/{paymentId}")
    suspend fun getPaymentMethodDetailsForId(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("paymentId") id: String
    ): Outcome<Exception, PaymentMethodDetailsResponse>

    @GET("payments/withdrawals/locks")
    fun getWithdrawalLocks(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("currency") currency: String
    ): Single<WithdrawalLocksResponse>
}
