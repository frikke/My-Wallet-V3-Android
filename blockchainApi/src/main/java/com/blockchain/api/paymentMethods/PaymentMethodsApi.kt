package com.blockchain.api.paymentMethods

import com.blockchain.api.paymentMethods.data.PaymentMethodDetailsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface PaymentMethodsApi {

    @GET("payments/beneficiaries/{paymentId}")
    fun getPaymentMethodDetailsForId(
        @Header("authorization") authorization: String,
        @Path("paymentId") id: String
    ): Single<PaymentMethodDetailsResponse>
}