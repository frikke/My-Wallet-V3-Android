package com.blockchain.core.walletoptions

import com.blockchain.core.auth.WalletAuthService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import info.blockchain.wallet.api.data.WalletOptions

class WalletOptionsStore(private val walletAuthService: WalletAuthService) :
    Store<WalletOptions> by InMemoryCacheStoreBuilder().build(
        storeId = "WalletOptionsStore",
        fetcher = Fetcher.ofSingle {
            walletAuthService.getWalletOptions().firstOrError()
        },
        mediator = FreshnessMediator(Freshness.ofHours(24))
    )
