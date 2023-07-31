package com.blockchain.core.chains.ethereum

import info.blockchain.balance.CoinNetwork
import io.reactivex.rxjava3.core.Single

interface EvmNetworksService {
    fun allEvmNetworks(): Single<List<CoinNetwork>>
}
