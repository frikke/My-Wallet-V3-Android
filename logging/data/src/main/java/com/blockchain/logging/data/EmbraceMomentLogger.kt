package com.blockchain.logging.data

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import io.embrace.android.embracesdk.Embrace

internal class EmbraceMomentLogger(
    private val embraceFeatureFlag: FeatureFlag
) : MomentLogger {

    private val embrace
        get() = Embrace.getInstance()

    override fun startEvent(event: MomentEvent) {
        checkEmbrace {
            embrace.startEvent(event.value)
        }
    }

    override fun endEvent(event: MomentEvent) {
        checkEmbrace {
            embrace.endEvent(event.value)
        }
    }

    private fun checkEmbrace(event: () -> Unit) {
        embraceFeatureFlag.enabled
            .subscribe { enabled ->
                if (enabled) {
                    event()
                }
            }
    }
}
