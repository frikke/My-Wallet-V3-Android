package com.blockchain.core.chains.ethereum

import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.data.EthTransaction

class EthLastTxCache(private val ethAccountApi: EthAccountApi) :
    KeyedStore<String, EthTransaction> by InMemoryCacheStoreBuilder().buildKeyed(
        storeId = "EthLastTxCache",
        mediator = FreshnessMediator(Freshness.ofSeconds(60 * 5)),
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = { address: String ->
                ethAccountApi.getLastEthTransaction(listOf(address)).defaultIfEmpty(EthTransaction())
            }
        )
    )
