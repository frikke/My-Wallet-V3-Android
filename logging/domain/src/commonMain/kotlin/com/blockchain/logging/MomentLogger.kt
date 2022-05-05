package com.blockchain.logging

interface MomentLogger {
    fun startEvent(name: String)
    fun endEvent(name: String)
}
