package com.blockchain.core.chains

import info.blockchain.balance.CoinNetwork

data class EvmNetwork(
    override val networkTicker: String,
    override val nativeAsset: String,
    override val name: String,
    override val shortName: String,
    override val chainId: Int,
    override val nodeUrl: String,
    override val explorerUrl: String
) : CoinNetwork
