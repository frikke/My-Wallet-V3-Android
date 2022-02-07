package com.blockchain.payments.googlepay.interceptor

import android.app.Activity
import android.content.Intent
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptor.Companion.GOOGLE_PAY_REQUEST_CODE
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GooglePayResponseInterceptorImpl constructor(
    private val paymentDataMapper: PaymentDataMapper,
    private val coroutineContext: CoroutineContext
) : GooglePayResponseInterceptor {

    private var onPaymentDataReceived: OnPaymentDataReceivedListener? = null

    private var job: Job? = null

    private val exceptionHandler =
        CoroutineExceptionHandler { _: CoroutineContext, throwable: Throwable ->
            onPaymentDataReceived?.onError(throwable)
        }

    override fun setPaymentDataReceivedListener(onPaymentDataReceivedListener: OnPaymentDataReceivedListener) {
        this.onPaymentDataReceived = onPaymentDataReceivedListener
    }

    override fun interceptActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GOOGLE_PAY_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK ->
                    data?.let { intent ->
                        paymentDataMapper.getPaymentDataFromIntent(intent)?.let(::handlePaymentSuccess)
                    }
                Activity.RESULT_CANCELED -> {
                    // Nothing to do here normally - the user simply cancelled without selecting a payment method.
                    onPaymentDataReceived?.onPaymentCancelled()
                }

                AutoResolveHelper.RESULT_ERROR -> {
                    AutoResolveHelper.getStatusFromIntent(data)?.let {
                        handleError(it.statusCode)
                    }
                }
            }
            onPaymentDataReceived?.onPaymentSheetClosed()
        }
    }

    override fun clear() {
        job?.cancel()
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        job = CoroutineScope(coroutineContext + exceptionHandler).launch {

            val response = paymentDataMapper.getPaymentDataResponse(paymentData)
            // If the gateway is set to "example", no payment information is returned - instead, the
            // token will only consist of "examplePaymentMethodToken".

            withContext(Dispatchers.Main) {
                if (
                    response.paymentMethodData.type == "PAYMENT_GATEWAY" &&
                    response.paymentMethodData.tokenizationData.token == "examplePaymentMethodToken"
                ) {
                    onPaymentDataReceived?.onError(Exception("Gateway name set to \\\"example\\\" - please modify."))
                } else {
                    onPaymentDataReceived?.onPaymentTokenReceived(response.paymentMethodData.tokenizationData.token)
                }
            }
        }
    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
     * WalletConstants.ERROR_CODE_* constants.
     * @see [
     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
     */
    private fun handleError(statusCode: Int) {
        onPaymentDataReceived?.onError(Exception(String.format("Error code: %d", statusCode)))
    }
}
