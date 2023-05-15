package com.blockchain.bitpay

import com.blockchain.bitpay.models.BitPayChain
import com.blockchain.bitpay.models.BitPaymentRequest
import com.blockchain.bitpay.models.RawPaymentRequest
import com.blockchain.bitpay.models.exceptions.wrapErrorMessage
import com.blockchain.enviroment.EnvironmentConfig
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class BitPayService constructor(
    environmentConfig: EnvironmentConfig,
    private val service: BitPay
) {

    private val baseUrl: String = environmentConfig.bitpayUrl

    internal fun getRawPaymentRequest(
        path: String = "$baseUrl$PATH_BITPAY_INVOICE",
        invoiceId: String,
        chain: String
    ): Single<RawPaymentRequest> = service.getRawPaymentRequest("$path/$invoiceId", BitPayChain(chain))
        .wrapErrorMessage()

    internal fun getPaymentVerificationRequest(
        path: String = "$baseUrl$PATH_BITPAY_INVOICE",
        body: BitPaymentRequest,
        invoiceId: String
    ): Completable =
        service.paymentRequest(
            path = "$path/$invoiceId",
            body = body,
            contentType = "application/payment-verification"
        )

    internal fun getPaymentSubmitRequest(
        path: String = "$baseUrl$PATH_BITPAY_INVOICE",
        body: BitPaymentRequest,
        invoiceId: String
    ): Completable = service.paymentRequest(
        path = "$path/$invoiceId",
        body = body,
        contentType = "application/payment"
    )
}
