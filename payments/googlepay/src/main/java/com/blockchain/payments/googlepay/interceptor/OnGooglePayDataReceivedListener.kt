package com.blockchain.payments.googlepay.interceptor

interface OnGooglePayDataReceivedListener {
    fun onGooglePayTokenReceived(token: String)
    fun onGooglePayCancelled()
    fun onGooglePaySheetClosed()
    fun onGooglePayError(e: Throwable)
}
