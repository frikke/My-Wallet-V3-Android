package com.blockchain.nabu.api.kyc.data.store

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.TierResponse
import com.blockchain.nabu.models.responses.nabu.TiersResponse
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

internal class KycStore(
    private val endpoint: Nabu,
    private val authenticator: Authenticator,
) : Store<TiersResponse> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                authenticator.authenticate {
                    endpoint.getTiers(it.authHeader)
                }
            }
        ),
        dataSerializer = TiersResponse.serializer(),
        mediator = object : Mediator<Unit, TiersResponse> {
            /**
             * 30 seconds for Pending, UnderReview
             * 1 hour for Verified
             * 1 hour else
             */
            fun shouldFetch(tiersResponse: List<TierResponse>, dataAgeMillis: Long): Boolean {
                return when {
                    tiersResponse.any {
                        it.state == KycTierState.Pending || it.state == KycTierState.UnderReview
                    } -> {
                        dataAgeMillis > TimeUnit.SECONDS.toMillis(30L)
                    }

                    tiersResponse[KycTierLevel.GOLD.ordinal].state == KycTierState.Verified -> {
                        dataAgeMillis > TimeUnit.HOURS.toMillis(1L)
                    }

                    else -> {
                        dataAgeMillis > TimeUnit.HOURS.toMillis(1L)
                    }
                }
            }

            override fun shouldFetch(cachedData: CachedData<Unit, TiersResponse>?): Boolean {
                cachedData ?: return true

                return shouldFetch(
                    tiersResponse = cachedData.data.tiers,
                    dataAgeMillis = Calendar.getInstance().timeInMillis - cachedData.lastFetched
                )
            }
        }
    ),
    KycDataSource {

    override fun stream(refresh: Boolean): Flow<StoreResponse<TiersResponse>> =
        stream(StoreRequest.Cached(forceRefresh = refresh))

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "KycStore"
    }
}
