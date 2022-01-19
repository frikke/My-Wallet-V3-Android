package com.blockchain.payments.stripe

import android.content.Context
import com.stripe.android.Stripe

class StripeFactory(
    private val context: Context,
    private val stripeAccountId: String? = null,
    private val enableLogging: Boolean = true
) {
    // We might be running multiple instances, one for each region/country (US, UK, EU).
    // The API Keys are coming from the payments/card-acquirer endpoint
    private val stripeInstances = mutableMapOf<String, Stripe>()

    fun getOrCreate(apiKey: String): Stripe {
        return stripeInstances[apiKey]
            ?: Stripe(
                context = context,
                publishableKey = apiKey,
                stripeAccountId = stripeAccountId,
                enableLogging = enableLogging
            ).also { stripe ->
                stripeInstances[apiKey] = stripe
            }
    }
}