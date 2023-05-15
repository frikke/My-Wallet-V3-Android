package com.blockchain.bitpay

import com.blockchain.bitpay.models.BitPaymentRequest
import com.blockchain.bitpay.models.RawPaymentRequest
import com.blockchain.core.utils.schedulers.applySchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.Locale

class BitPayDataManager constructor(
    private val bitPayService: BitPayService
) {

    /**
     * Returns a [RawPaymentRequest] object containing details about the BitPay invoice payment
     * request
     *
     * @param invoiceId The id of the BitPay invoice
     *
     * @return A [RawPaymentRequest] object wrapped in a [Single].
     */
    fun getRawPaymentRequest(invoiceId: String, currencyCode: String): Single<RawPaymentRequest> =
        bitPayService.getRawPaymentRequest(
            invoiceId = invoiceId,
            chain = currencyCode.toUpperCase(Locale.getDefault())
        ).applySchedulers()

    fun paymentVerificationRequest(invoiceId: String, paymentRequest: BitPaymentRequest): Completable =
        bitPayService.getPaymentVerificationRequest(
            invoiceId = invoiceId,
            body = paymentRequest
        ).applySchedulers()

    fun paymentSubmitRequest(invoiceId: String, paymentRequest: BitPaymentRequest): Completable =
        bitPayService.getPaymentSubmitRequest(
            invoiceId = invoiceId,
            body = paymentRequest
        ).applySchedulers()
}
