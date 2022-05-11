package com.blockchain.coincore.eth

import com.blockchain.core.chains.EvmNetwork

interface MultiChainAccount {
    val l1Network: EvmNetwork
}
