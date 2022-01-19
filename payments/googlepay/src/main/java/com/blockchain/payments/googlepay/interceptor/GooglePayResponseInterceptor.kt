package com.blockchain.payments.googlepay.interceptor

import android.content.Intent

interface GooglePayResponseInterceptor {

    companion object {
        const val GOOGLE_PAY_REQUEST_CODE = 991
    }

    fun setPaymentDataReceivedListener(onPaymentDataReceivedListener: OnPaymentDataReceivedListener)

    fun interceptActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    fun clear()
}