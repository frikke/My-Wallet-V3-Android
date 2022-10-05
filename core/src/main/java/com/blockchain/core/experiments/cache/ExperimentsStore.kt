package com.blockchain.core.experiments.cache

import com.blockchain.api.services.ExperimentsApiService
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

class ExperimentsStore(
    private val experimentsApiService: ExperimentsApiService
) : Store<Map<String, Int>> by InMemoryCacheStoreBuilder().build(
    storeId = STORE_ID,
    fetcher = Fetcher.ofSingle(
        mapper = {
            rxSingleOutcome {
                experimentsApiService.getExperiments()
                //                {
                //                    "experiment-1" : 2
                //                    "experiment-2" : 1
                //                    "experiment-3" : 4
                //                    ...
                //                }
            }
        },
    ),
    mediator = FreshnessMediator(Freshness.ofMinutes(60L))
) {

    companion object {
        private const val STORE_ID = "ExperimentsStore"
    }
}
