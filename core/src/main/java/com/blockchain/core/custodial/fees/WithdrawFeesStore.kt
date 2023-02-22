package com.blockchain.core.custodial.fees

import com.blockchain.api.fees.WithdrawFeesAndMinLimitResponse
import com.blockchain.api.fees.WithdrawFeesAndMinRequest
import com.blockchain.api.fees.WithdrawFeesService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder

class WithdrawFeesStore(private val withdrawFeesService: WithdrawFeesService) : KeyedStore<
    WithdrawFeesAndMinRequest,
    WithdrawFeesAndMinLimitResponse> by InMemoryCacheStoreBuilder().buildKeyed(
    storeId = "WithdrawFeesStoreCache",
    fetcher = Fetcher.Keyed.ofSingle(
        mapper = { withdrawFeesAndMinRequest: WithdrawFeesAndMinRequest ->
            withdrawFeesService.withdrawFeesAndMinLimit(withdrawFeesAndMinRequest)
        }
    ),
    mediator = FreshnessMediator(Freshness.ofMinutes(5))
)
