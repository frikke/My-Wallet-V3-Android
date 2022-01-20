package com.blockchain.payments.googlepay.manager

import android.app.Activity
import android.util.Log
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptor
import com.blockchain.payments.googlepay.manager.request.GooglePayRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GooglePayManagerImpl : GooglePayManager {

    companion object {
        /**
         * Changing this to ENVIRONMENT_PRODUCTION will make the API return chargeable card information.
         * Please refer to the documentation to read about the required steps needed to enable
         * ENVIRONMENT_PRODUCTION.
         *
         * @value #PAYMENTS_ENVIRONMENT
         */
        private const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST
    }

    private lateinit var paymentsClient: PaymentsClient

    @ExperimentalSerializationApi
    private val jsonBuilder by lazy {
        Json {
            explicitNulls = false
            encodeDefaults = true
        }
    }

    override fun initManager(activity: Activity) {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(PAYMENTS_ENVIRONMENT)
            .build()

        paymentsClient = Wallet.getPaymentsClient(activity, walletOptions)
    }

    override fun checkIfGooglePayIsAvailable(googlePayRequest: GooglePayRequest, isAvailable: (Boolean) -> Unit) {
        val request = IsReadyToPayRequest.fromJson(jsonBuilder.encodeToString(googlePayRequest))

        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                completedTask.getResult(ApiException::class.java)?.let { isAvailable(it) }
            } catch (exception: ApiException) {
                // Process error
                Log.w("isReadyToPay failed", exception)
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
