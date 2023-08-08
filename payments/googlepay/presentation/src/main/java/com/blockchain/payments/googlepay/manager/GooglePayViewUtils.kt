package com.blockchain.payments.googlepay.manager

import android.app.Activity
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptor
import com.blockchain.payments.googlepay.manager.request.GooglePayRequest
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GooglePayViewUtils(
    private val paymentsClient: PaymentsClient,
    private val json: Json
) {
    fun requestPayment(googlePayRequest: GooglePayRequest, activity: Activity) {
        val request = PaymentDataRequest.fromJson(json.encodeToString(googlePayRequest))

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            activity,
            GooglePayResponseInterceptor.GOOGLE_PAY_REQUEST_CODE
        )
    }
}
