package com.blockchain.payments.googlepay.interceptor

interface OnPaymentDataReceivedListener {
    fun onPaymentTokenReceived(token: String)
    fun onPaymentCancelled()
    fun onPaymentSheetClosed()
    fun onError(e: Throwable)
}