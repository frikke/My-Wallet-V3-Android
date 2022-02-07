package com.blockchain.payments.googlepay.manager

import android.app.Activity
import com.blockchain.payments.googlepay.manager.request.GooglePayRequest

interface GooglePayManager {
    fun initManager(activity: Activity)
    fun checkIfGooglePayIsAvailable(googlePayRequest: GooglePayRequest, isAvailable: (Boolean) -> Unit)
    fun requestPayment(googlePayRequest: GooglePayRequest, activity: Activity)
}
