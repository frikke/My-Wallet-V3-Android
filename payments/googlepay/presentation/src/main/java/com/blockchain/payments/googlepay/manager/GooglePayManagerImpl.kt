package com.blockchain.payments.googlepay.manager

import android.util.Log
import com.blockchain.payments.googlepay.manager.request.GooglePayRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GooglePayManagerImpl(
    private val paymentsClient: PaymentsClient,
    private val json: Json
) : GooglePayManager {

    override suspend fun checkIfGooglePayIsAvailable(googlePayRequest: GooglePayRequest): Boolean =
        suspendCoroutine { continuation ->
            val request = IsReadyToPayRequest.fromJson(json.encodeToString(googlePayRequest))
            // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
            // OnCompleteListener to be triggered when the result of the call is known.
            val task = paymentsClient.isReadyToPay(request)
            task.addOnCompleteListener { completedTask ->
                try {
                    completedTask.getResult(ApiException::class.java)?.let { continuation.resume(it) }
                } catch (exception: ApiException) {
                    Log.w("isReadyToPay failed", exception)
                    continuation.resume(false)
                }
            }
        }
}
