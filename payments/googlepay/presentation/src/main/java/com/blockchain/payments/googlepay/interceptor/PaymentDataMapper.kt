package com.blockchain.payments.googlepay.interceptor

import android.content.Intent
import com.blockchain.payments.googlepay.interceptor.response.PaymentDataResponse
import com.google.android.gms.wallet.PaymentData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class PaymentDataMapper {

    fun getPaymentDataFromIntent(intent: Intent): PaymentData? = PaymentData.getFromIntent(intent)

    fun getPaymentDataResponse(paymentData: PaymentData): PaymentDataResponse =
        Json.decodeFromString(paymentData.toJson())
}
