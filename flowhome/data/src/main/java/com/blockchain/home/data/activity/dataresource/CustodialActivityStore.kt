package com.blockchain.home.data.activity.dataresource

import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.Coincore
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import com.blockchain.walletmode.WalletMode

class CustodialActivityStore(
    private val coincore: Coincore
) : Store<ActivitySummaryList> by InMemoryCacheStoreBuilder().build(
    storeId = "CustodialActivityStore",
    fetcher = Fetcher.ofSingle(
        mapper = {
            println("------------- interestService.getActivity Store call")

            coincore.allWalletsInMode(WalletMode.CUSTODIAL_ONLY)
                .flatMap { accountGroup ->
                    accountGroup.activity
                }
                .map {
                    println("------------- interestService.getActivity Store map ${it}")
                    it
                }
                .doOnError {
                    println("------------- interestService.getActivity Store doOnError ${it}")
                }
        }
    ),
    mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
)
