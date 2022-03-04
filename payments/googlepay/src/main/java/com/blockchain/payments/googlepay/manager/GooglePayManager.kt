package com.blockchain.payments.googlepay.manager

import android.app.Activity
import com.blockchain.payments.googlepay.manager.request.GooglePayRequest

interface GooglePayManager {
    suspend fun checkIfGooglePayIsAvailable(googlePayRequest: GooglePayRequest): Boolean
    fun requestPayment(googlePayRequest: GooglePayRequest, activity: Activity)
}
