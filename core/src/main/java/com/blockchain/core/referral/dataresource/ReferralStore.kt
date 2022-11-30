package com.blockchain.core.referral.dataresource

import com.blockchain.api.referral.data.ReferralResponse
import com.blockchain.api.services.ReferralApiService
import com.blockchain.outcome.map
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.serialization.Serializable

class ReferralStore(
    private val referralApi: ReferralApiService
) : KeyedStore<ReferralStore.Key, ReferralResponseWrapper> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = "ReferralStore",
        fetcher = Fetcher.Keyed.ofOutcome { key ->
            referralApi.getReferralCode(key.fiatTicker).map {
                ReferralResponseWrapper(it)
            }
        },
        keySerializer = Key.serializer(),
        dataSerializer = ReferralResponseWrapper.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ) {

    @Serializable
    data class Key(
        val fiatTicker: String
    )
}

@Serializable
data class ReferralResponseWrapper(
    val referralResponse: ReferralResponse?
)