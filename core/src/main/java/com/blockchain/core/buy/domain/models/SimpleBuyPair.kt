package com.blockchain.core.buy.domain.models

data class SimpleBuyPair(
    val pair: Pair<String, String>,
    val buyMin: Long,
    val buyMax: Long,
    val sellMin: Long,
    val sellMax: Long
)