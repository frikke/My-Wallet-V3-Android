package com.blockchain.nabu.api.kyc.data.datasources

import com.blockchain.api.kyc.KycApiService
import com.blockchain.api.kyc.model.KycTierDto
import com.blockchain.api.kyc.model.KycTiersDto
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.kyc.domain.model.KycTierLevel
import com.blockchain.nabu.api.kyc.domain.model.KycTierState
import com.blockchain.store.CachedData
import com.blockchain.store.Fetcher
import com.blockchain.store.Mediator
import com.blockchain.store.Store
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import java.util.Calendar
import java.util.concurrent.TimeUnit

class KycTiersStore(
    private val kycApiService: KycApiService,
    private val authenticator: Authenticator,
) : Store<KycTiersDto> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofSingle(
            mapper = {
                authenticator.authenticate {
                    kycApiService.getTiers(it.authHeader)
                }
            }
        ),
        dataSerializer = KycTiersDto.serializer(),
        mediator = object : Mediator<Unit, KycTiersDto> {
            /**
             * 30 seconds for Pending, UnderReview
             * 1 hour for Verified
             * 1 hour else
             */
            fun shouldFetch(tiersResponse: List<KycTierDto>, dataAgeMillis: Long): Boolean {
                return when {
                    tiersResponse.any {
                        KycTierState.fromValue(it.state) == KycTierState.Pending ||
                            KycTierState.fromValue(it.state) == KycTierState.UnderReview
                    } -> {
                        dataAgeMillis > TimeUnit.SECONDS.toMillis(30L)
                    }

                    KycTierState.fromValue(tiersResponse[KycTierLevel.GOLD.ordinal].state) == KycTierState.Verified -> {
                        dataAgeMillis > TimeUnit.HOURS.toMillis(1L)
                    }

                    else -> {
                        dataAgeMillis > TimeUnit.HOURS.toMillis(1L)
                    }
                }
            }

            override fun shouldFetch(cachedData: CachedData<Unit, KycTiersDto>?): Boolean {
                cachedData ?: return true

                return shouldFetch(
                    tiersResponse = cachedData.data.tiers,
                    dataAgeMillis = Calendar.getInstance().timeInMillis - cachedData.lastFetched
                )
            }
        }
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "GetKycTiersStore"
    }
}
