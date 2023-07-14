package com.blockchain.presentation.spinner

import androidx.lifecycle.DefaultLifecycleObserver

// another idea - if we need detailed actions

/*sealed class SpinnerAnalyticsScreen(val name: String) {
    object AddCard : SpinnerAnalyticsScreen("AddCard") {
        object Default: SpinnerAnalyticsAction("Default")
    }

    object Buy : SpinnerAnalyticsScreen("Buy") {
        object LoadPaymentCards: SpinnerAnalyticsAction("LoadPaymentCards")
    }
}

sealed class SpinnerAnalyticsAction(val name: String)*/

enum class SpinnerAnalyticsScreen {
    AddCard, BuyCheckout, BuyOrder
}

enum class SpinnerAnalyticsAction {
    Default
}

/**
 * DO NOT FORGET lifecycle.addObserver(spinnerTimer) in the fragment/activity/compose
 * to be able to call stop when the screen is destroyed
 */
interface SpinnerAnalyticsTimer : DefaultLifecycleObserver{
    fun start(action: SpinnerAnalyticsAction)
    fun stop(isDestroyed: Boolean)
    fun backgrounded()
}
