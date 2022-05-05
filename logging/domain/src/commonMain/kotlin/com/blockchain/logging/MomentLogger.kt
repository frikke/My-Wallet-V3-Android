package com.blockchain.logging

interface MomentLogger {
    fun startEvent(event: MomentEvent)
    fun endEvent(event: MomentEvent)
}

enum class MomentEvent(val value: String) {
    PIN_TO_DASHBOARD("Pin->Dashboard")
}
