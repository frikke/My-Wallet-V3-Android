package com.blockchain.bitpay

import com.blockchain.bitpay.models.BitPayChain
import com.blockchain.bitpay.models.BitPaymentRequest
import com.blockchain.bitpay.models.RawPaymentRequest
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Url

interface BitPay {

    @POST
    @Headers(
        "BP_PARTNER: Blockchain",
        "BP_PARTNER_VERSION: V6.28.0",
        "Accept: application/payment-options",
        "Content-Type: application/payment-request",
        "x-paypro-version: 2"
    )
    fun getRawPaymentRequest(
        @Url path: String,
        @Body body: BitPayChain
    ): Single<RawPaymentRequest>

    @POST
    @Headers(
        "BP_PARTNER: Blockchain",
        "BP_PARTNER_VERSION: V6.28.0",
        "x-paypro-version: 2"
    )
    fun paymentRequest(
        @Url path: String,
        @Body body: BitPaymentRequest,
        @Header("Content-Type") contentType: String
    ): Completable
}
