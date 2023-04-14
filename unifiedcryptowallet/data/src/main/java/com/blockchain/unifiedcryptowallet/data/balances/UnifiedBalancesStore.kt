package com.blockchain.unifiedcryptowallet.data.balances

import com.blockchain.api.selfcustody.BalancesResponse
import com.blockchain.api.selfcustody.CommonResponse
import com.blockchain.api.selfcustody.SubscriptionInfo
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.outcome.doOnSuccess
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.impl.IsCachedMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.ListSerializer

class UnifiedBalancesStore(
    private val selfCustodyService: DynamicSelfCustodyService,
    private val currencyPrefs: CurrencyPrefs
) : Store<BalancesResponse> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                selfCustodyService.getBalances(
                    fiatCurrency = currencyPrefs.selectedFiatCurrency.networkTicker
                )
            }
        ),
        dataSerializer = BalancesResponse.serializer(),
        mediator = IsCachedMediator()
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "UnifiedBalancesStoreStore"
    }
}

internal class UnifiedBalancesSubscribeStore(
    private val selfCustodyService: DynamicSelfCustodyService,
    private val unifiedBalancesStore: UnifiedBalancesStore,
) : KeyedStore<List<SubscriptionInfo>, CommonResponse> by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
    storeId = STORE_ID,
    fetcher = Fetcher.Keyed.ofOutcome(
        mapper = { key ->
            selfCustodyService.subscribe(
                data = key,
            ).doOnSuccess {
                unifiedBalancesStore.invalidate()
            }
        }
    ),
    keySerializer = ListSerializer(SubscriptionInfo.serializer()),
    dataSerializer = CommonResponse.serializer(),
    mediator = object : Mediator<List<SubscriptionInfo>, CommonResponse> {
        override fun shouldFetch(cachedData: CachedData<List<SubscriptionInfo>, CommonResponse>?): Boolean {
            return cachedData == null || cachedData.lastFetched == 0L
        }
    }
) {
    companion object {
        private const val STORE_ID = "UnifiedBalancesSubscribeStore"
    }
}
