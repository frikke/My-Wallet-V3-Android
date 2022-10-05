package com.blockchain.payments.googlepay.manager

import com.blockchain.payments.googlepay.manager.request.GooglePayRequest

interface GooglePayManager {
    suspend fun checkIfGooglePayIsAvailable(googlePayRequest: GooglePayRequest): Boolean
}
