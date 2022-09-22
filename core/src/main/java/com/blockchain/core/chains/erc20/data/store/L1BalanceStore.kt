package com.blockchain.core.chains.erc20.data.store

import com.blockchain.logging.RemoteLogger
import com.blockchain.outcome.Outcome
import com.blockchain.serializers.BigIntSerializer
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.KeyedFlushableDataSource
import java.math.BigInteger
import kotlinx.serialization.Serializable
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import timber.log.Timber

/**
 * todo(othman) related stores and (ERC20) managers should be refactored
 */
class L1BalanceStore(
    private val ethDataManager: EthDataManager,
    private val remoteLogger: RemoteLogger
) : KeyedStore<L1BalanceStore.Key, BigInteger> by PersistedJsonSqlDelightStoreBuilder()
    .buildKeyed(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = { key ->
                try {
                    ethDataManager.getBalance(key.nodeUrl)
                } catch (ex: Exception) {
                    // Sometimes this gets called before the Ethereum Wallet is initialised which leads to a fatal crash
                    // Returning an Outcome error means the relevant error messages will be shown on the Dashboard
                    remoteLogger.logException(ex)
                    Timber.e(ex)
                    Outcome.Failure(ex)
                }
            }
        ),
        keySerializer = Key.serializer(),
        dataSerializer = BigIntSerializer,
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    KeyedFlushableDataSource<L1BalanceStore.Key> {

    @Serializable
    data class Key(
        val nodeUrl: String
    )

    override fun invalidate(param: Key) {
        markAsStale(param)
    }

    override fun invalidate() {
        markStoreAsStale()
    }

    companion object {
        private const val STORE_ID = "L1BalanceStore"
    }
}
