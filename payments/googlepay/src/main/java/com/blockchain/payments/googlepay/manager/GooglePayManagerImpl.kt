package com.blockchain.payments.googlepay.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptor
import com.blockchain.payments.googlepay.manager.request.GooglePayRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GooglePayManagerImpl(
    environmentConfig: EnvironmentConfig,
    context: Context
) : GooglePayManager {

    /**
     * Changing this to ENVIRONMENT_PRODUCTION will make the API return chargeable card information.
     * Please refer to the documentation to read about the required steps needed to enable
     * ENVIRONMENT_PRODUCTION.
     *
     * @value #PAYMENTS_ENVIRONMENT
     */
    private val paymentsEnvironment: Int by lazy {
        if (environmentConfig.environment == Environment.PRODUCTION) {
            WalletConstants.ENVIRONMENT_PRODUCTION
        } else {
            WalletConstants.ENVIRONMENT_TEST
        }
    }

    private val paymentsClient: PaymentsClient by lazy {
        Wallet.getPaymentsClient(
            context,
            Wallet.WalletOptions.Builder()
                .setEnvironment(paymentsEnvironment)
                .build()
        )
    }

    @ExperimentalSerializationApi
    private val jsonBuilder by lazy {
        Json {
            explicitNulls = false
            encodeDefaults = true
        }
    }

    override suspend fun checkIfGooglePayIsAvailable(googlePayRequest: GooglePayRequest): Boolean =
        suspendCoroutine { continuation ->
            val request = IsReadyToPayRequest.fromJson(jsonBuilder.encodeToString(googlePayRequest))
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

    override fun requestPayment(googlePayRequest: GooglePayRequest, activity: Activity) {
        val request = PaymentDataRequest.fromJson(jsonBuilder.encodeToString(googlePayRequest))

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request), activity,
            GooglePayResponseInterceptor.GOOGLE_PAY_REQUEST_CODE
        )
    }
}
