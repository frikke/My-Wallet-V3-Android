package com.blockchain.analytics

interface TraitsService {
    suspend fun traits(): Map<String, String>
}
