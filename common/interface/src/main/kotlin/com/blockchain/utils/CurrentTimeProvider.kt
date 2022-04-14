package com.blockchain.utils

/**
 * Wrapper around System because we can't mock System in tests.
 * This should be used in favor of System.currentTimeMillis() on classes that need to test time sensitive behaviour
 */
object CurrentTimeProvider {
    fun currentTimeMillis(): Long = System.currentTimeMillis()
}
