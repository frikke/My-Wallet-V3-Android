package com.blockchain.logging.data

import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import com.blockchain.logging.MomentParam
import io.embrace.android.embracesdk.Embrace

internal object EmbraceMomentLogger : MomentLogger {
    private val embrace
        get() = Embrace.getInstance()

    override fun startEvent(event: MomentEvent) {
        embrace.startEvent(event.value)
    }

    override fun endEvent(event: MomentEvent, params: Map<MomentParam, String>) {
        embrace.endEvent(event.value, params.mapKeys { it.key.value })
    }
}
