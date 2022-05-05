package com.blockchain.logging

interface MomentLogger {
    fun startEvent(name: String)
    fun endEvent(name: String)
}

object MomentEvent {
    const val PIN_TO_DASHBOARD = "Pin->Dashboard"
}