package com.blockchain.coincore.eth

interface MultiChainAccount {
    val chainNetworkTicker: String
    val chainId: Int
    val networkName: String
}
