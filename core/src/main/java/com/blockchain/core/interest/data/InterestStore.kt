package com.blockchain.core.interest.data

import com.blockchain.api.services.InterestApiService
import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.ListSerializer

class InterestStore(
    private val interestApiService: InterestApiService,
    private val authenticator: Authenticator
) : Store< List<InterestBalanceDetails>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                authenticator.authenticate {
                    interestApiService.getAllInterestAccountBalances(it.authHeader)
                }
            }
        ),
        dataSerializer = ListSerializer(InterestBalanceDetails.serializer()),
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
