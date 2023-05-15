package com.blockchain.testutils

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

class NoOpCoroutineDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // no op
    }
}
