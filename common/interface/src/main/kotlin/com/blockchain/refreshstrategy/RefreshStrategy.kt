package com.blockchain.refreshstrategy

sealed interface RefreshStrategy {
    /**
     * To get cached data + refresh and return new data
     */
    data class Cached(val refresh: Boolean) : RefreshStrategy

    /**
     * To ignore cache and get fresh data from remote directly
     */
    object Fresh : RefreshStrategy
}