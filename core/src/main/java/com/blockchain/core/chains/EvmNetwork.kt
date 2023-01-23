package com.blockchain.core.chains

data class EvmNetwork(
    val networkTicker: String,
    val nativeAsset: String,
    val networkName: String,
    val chainId: Int,
    val nodeUrl: String,
    val explorerUrl: String,
)
