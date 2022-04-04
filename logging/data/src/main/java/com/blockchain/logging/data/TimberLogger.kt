package com.blockchain.logging.data

import com.blockchain.logging.Logger
import timber.log.Timber

internal class TimberLogger : Logger {

    override fun d(s: String) {
        Timber.d(s)
    }

    override fun v(s: String) {
        Timber.v(s)
    }

    override fun e(s: String) {
        Timber.e(s)
    }
}
