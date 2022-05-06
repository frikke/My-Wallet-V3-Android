package com.blockchain.logging.data

import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import io.embrace.android.embracesdk.Embrace

internal object EmbraceMomentLogger : MomentLogger {
    private val embrace
        get() = Embrace.getInstance()

    override fun startEvent(event: MomentEvent) {
        embrace.startEvent(event.value)
    }

    override fun endEvent(event: MomentEvent) {
        embrace.endEvent(event.value)
    }
}
