package com.blockchain.core.chains.dynamicselfcustody.data

import com.blockchain.api.selfcustody.GetSubscriptionsResponse
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.preferences.AuthPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import org.bitcoinj.core.Sha256Hash
import org.spongycastle.util.encoders.Hex

internal class NonCustodialSubscriptionsStore(
    private val dynamicSelfCustodyService: DynamicSelfCustodyService,
    private val authPrefs: AuthPrefs
) : Store<GetSubscriptionsResponse> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                dynamicSelfCustodyService.getSubscriptions(
                    guidHash = getHashedString(authPrefs.walletGuid),
                    sharedKeyHash = getHashedString(authPrefs.sharedKey)
                )
            }
        ),
        dataSerializer = GetSubscriptionsResponse.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "NonCustodialSubscriptionsStore"
    }
}

private fun getHashedString(input: String): String = String(Hex.encode(Sha256Hash.hash(input.toByteArray())))
