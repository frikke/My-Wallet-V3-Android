package com.blockchain.coincore.impl

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.logging.Logger
import com.blockchain.preferences.AuthPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.asObservable
import com.blockchain.store.impl.IsCachedMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class NotificationAddresses(
    val assetTicker: String,
    val addressList: List<String>,
)

@Serializable
internal data class NotificationReceiveAddresses(
    private val coin: String,
    private val addresses: List<String>,
)

// Update the BE with the current address sets for assets, used to
// send notifications back to the app when Tx's complete
internal class BackendNotificationUpdater(
    private val coinAddressesStore: CoinAddressesStore,
    private val json: Json
) {

    private val addressMap = mutableMapOf<String, NotificationAddresses>()

    fun updateNotificationBackend(item: NotificationAddresses) {
        addressMap[item.assetTicker] = item
        if (item.assetTicker in REQUIRED_ASSETS && requiredAssetsUpdated()) {
            // This is a fire and forget operation.
            // We don't want this call to delay the main rx chain, and we don't care about errors,
            updateBackend().ignoreElements()
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { Logger.e("Notification Update failed: $it") })
        }
    }

    private fun requiredAssetsUpdated(): Boolean {
        REQUIRED_ASSETS.forEach { if (!addressMap.containsKey(it)) return@requiredAssetsUpdated false }
        return true
    }

    private fun updateBackend() = coinAddressesStore.stream(
        FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale).withKey(
            coinReceiveAddresses()
        )
    ).asObservable()

    private fun coinReceiveAddresses(): String {
        val addresses = REQUIRED_ASSETS.map { key ->
            val addresses =
                addressMap[key]?.addressList ?: throw IllegalStateException("Required Asset missing")
            NotificationReceiveAddresses(key, addresses.sorted())
        }

        return json.encodeToString(addresses)
    }

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
            ).firstOrError().map { }
        },
        dataSerializer = Unit.serializer(),
        keySerializer = String.serializer(),
        mediator = IsCachedMediator()
    )
