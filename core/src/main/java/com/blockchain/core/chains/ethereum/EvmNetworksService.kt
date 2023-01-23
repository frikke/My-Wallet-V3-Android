package com.blockchain.core.chains.ethereum

import com.blockchain.core.chains.EvmNetwork
import io.reactivex.rxjava3.core.Single

interface EvmNetworksService {
    fun allEvmNetworks(): Single<List<EvmNetwork>>
}
