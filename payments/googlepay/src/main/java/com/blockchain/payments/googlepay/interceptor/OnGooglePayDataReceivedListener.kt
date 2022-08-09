package com.blockchain.payments.googlepay.interceptor

import com.blockchain.payments.googlepay.interceptor.response.PaymentDataResponse

interface OnGooglePayDataReceivedListener {
    fun onGooglePayTokenReceived(token: String, address: PaymentDataResponse.Address?)
    fun onGooglePayCancelled()
    fun onGooglePaySheetClosed()
    fun onGooglePayError(e: Throwable)
}
