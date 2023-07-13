package com.blockchain.spinner

enum class SpinnerAnalyticsScreen {
    AddCard
}

enum class SpinnerAnalyticsAction {
    Default
}

interface SpinnerAnalyticsTimer {
    fun start(action: SpinnerAnalyticsAction)
    fun stop()
    fun backgrounded()
}
