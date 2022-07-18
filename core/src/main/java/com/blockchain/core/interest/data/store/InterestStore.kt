package com.blockchain.core.interest.data.store

import com.blockchain.api.services.InterestApiService
import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer

internal class InterestStore(
    private val interestApiService: InterestApiService,
    private val authenticator: Authenticator
) : Store<Throwable, List<InterestBalanceDetails>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofSingle(
            mapper = {
                authenticator.authenticate {
                    interestApiService.getAllInterestAccountBalances(it.authHeader)
                }
            },
            errorMapper = { it }
        ),
        dataSerializer = ListSerializer(InterestBalanceDetails.serializer()),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    InterestDataSource {

    override fun stream(refresh: Boolean): Flow<StoreResponse<Throwable, List<InterestBalanceDetails>>> =
        stream(StoreRequest.Cached(refresh))

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "InterestStore"
    }
}
