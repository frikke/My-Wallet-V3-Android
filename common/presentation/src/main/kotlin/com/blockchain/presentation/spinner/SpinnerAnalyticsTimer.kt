package com.blockchain.presentation.spinner

import androidx.lifecycle.DefaultLifecycleObserver

// another idea - if we need detailed actions

sealed class SpinnerAnalyticsScreen(val name: String) {
    object AddCard : SpinnerAnalyticsScreen("AddCard")

    object BuyCheckout : SpinnerAnalyticsScreen("BuyCheckout") {
        object BuyButtonClick : SpinnerAnalyticsAction("BuyButtonClick")
        object GooglePayButtonClick : SpinnerAnalyticsAction("GooglePayButtonClick")
    }

    object BuyConfirmOrder : SpinnerAnalyticsScreen("BuyConfirmOrder")
}

sealed class SpinnerAnalyticsAction(val name: String) {
    object Default : SpinnerAnalyticsAction("Default")
}

/**
 * DO NOT FORGET lifecycle.addObserver(spinnerTimer) in the fragment/activity/compose
 * to be able to call stop when the screen is destroyed
 *
 * [start] the timer and it will send events every 5 seconds, when loading stop call [stop]
 *
 * by default the action of the loading would be [SpinnerAnalyticsAction.Default] means just screen starts
 * or default initial screen action
 *
 * if a loading is triggered by some other event you can [updateAction] and it will be sent in following events
 */
interface SpinnerAnalyticsTimer : DefaultLifecycleObserver {
    fun updateAction(action: SpinnerAnalyticsAction)
    fun start()
    fun stop(isDestroyed: Boolean = false)
}
