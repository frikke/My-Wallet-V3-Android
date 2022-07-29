package com.blockchain.core.interest.data.datasources

import com.blockchain.api.interest.InterestApiService
import com.blockchain.api.interest.data.InterestAccountBalanceDto
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class InterestBalancesStore(
    private val interestApiService: InterestApiService,
    private val authenticator: Authenticator
) : Store<Map<String, InterestAccountBalanceDto>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                authenticator.authenticate {
                    interestApiService.getAccountBalances(it.authHeader)
                }
            }
        ),
        dataSerializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = InterestAccountBalanceDto.serializer()
        ),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "InterestStore"
    }
}
