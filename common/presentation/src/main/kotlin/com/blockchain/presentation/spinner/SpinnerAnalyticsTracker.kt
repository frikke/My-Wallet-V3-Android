package com.blockchain.presentation.spinner

enum class SpinnerAnalyticsScreen {
    AddCard,
    BuyCheckout,
    BuyConfirmOrder
}

interface SpinnerAnalyticsTracker {
    fun start()
    fun stop()
}
