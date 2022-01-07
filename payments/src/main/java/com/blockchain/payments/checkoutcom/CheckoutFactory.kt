package com.blockchain.payments.checkoutcom

import android.content.Context
import com.checkout.android_sdk.CheckoutAPIClient
import com.checkout.android_sdk.Utils.Environment

class CheckoutFactory(
    private val context: Context,
    private val isProd: Boolean
) {
    private val environment: Environment by lazy {
        if (isProd) {
            Environment.LIVE
        } else {
            Environment.SANDBOX
        }
    }
    private val checkoutInstances: MutableMap<String, CheckoutAPIClient> = mutableMapOf()

    fun getOrCreate(apiKey: String): CheckoutAPIClient {
        return checkoutInstances[apiKey] ?:
        CheckoutAPIClient(
            context,
            apiKey,
            environment
        ).also { checkoutApiClient ->
            checkoutInstances[apiKey] = checkoutApiClient
        }
    }
}