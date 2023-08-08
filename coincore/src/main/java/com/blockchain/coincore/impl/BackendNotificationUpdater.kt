package com.blockchain.coincore.impl

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.toObservable
import com.blockchain.preferences.AuthPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.IsCachedMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Completable
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class NotificationAddresses(
    val assetTicker: String,
    val addressList: List<String>
)

// Update the BE with the current address sets for assets, used to
// send notifications back to the app when Tx's complete
internal class BackendNotificationUpdater(
    private val coinAddressesStore: CoinAddressesStore,
    private val json: Json
) {

    fun updateNotificationBackend(item: NotificationAddresses): Completable {
        return if (item.assetTicker in REQUIRED_ASSETS) {
            updateBackend(item).ignoreElement().onErrorComplete()
        } else Completable.complete()
    }

    private fun updateBackend(item: NotificationAddresses) = coinAddressesStore.stream(
        FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale).withKey(
            json.encodeToString(item.copy(addressList = item.addressList.sorted()))
        )
    ).toObservable().firstOrError()

    companion object {
        private val REQUIRED_ASSETS = setOf(
            CryptoCurrency.BTC.networkTicker,
            CryptoCurrency.BCH.networkTicker,
            CryptoCurrency.ETHER.networkTicker
        )
    }
}

class CoinAddressesStore(private val walletApi: WalletApi, private val prefs: AuthPrefs) :
    KeyedStore<String, Unit> by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
        storeId = "CoinAddressesStore",
        fetcher = Fetcher.Keyed.ofSingle { key ->
            walletApi.submitCoinReceiveAddresses(
                prefs.walletGuid,
                prefs.sharedKey,
                key
            ).firstOrError().map { Unit }
        },
        dataSerializer = Unit.serializer(),
        keySerializer = String.serializer(),
        mediator = IsCachedMediator()
    )
