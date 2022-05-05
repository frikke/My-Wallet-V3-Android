package com.blockchain.logging.data

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.MomentLogger
import io.embrace.android.embracesdk.Embrace

internal class EmbraceMomentLogger(
    private val embraceFeatureFlag: FeatureFlag
) : MomentLogger {

    private val embrace
        get() = Embrace.getInstance()

    override fun startEvent(name: String) {
        checkEmbrace {
            embrace.startEvent(name)
        }
    }

    override fun endEvent(name: String) {
        checkEmbrace {
            embrace.endEvent(name)
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
