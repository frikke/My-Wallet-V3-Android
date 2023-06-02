package com.blockchain.notifications

import com.blockchain.store.CacheConfiguration
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.IsCachedMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import info.blockchain.wallet.api.WalletApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer

class NotificationStorage(private val walletApi: WalletApi) :
    KeyedStore<TokenCredentials, Unit> by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
        storeId = "NotificationStorage",
        reset = CacheConfiguration.onLogout(),
        fetcher = Fetcher.Keyed.ofSingle { key ->
            walletApi.updateFirebaseNotificationToken(token = key.token, sharedKey = key.sharedKey, guid = key.guid)
                .onErrorComplete()
                .toSingle { }
        },
        dataSerializer = Unit.serializer(),
        keySerializer = TokenCredentials.serializer(),
        mediator = IsCachedMediator()
    )

@Serializable
data class TokenCredentials(
    val token: String,
    @Transient
    val sharedKey: String = "",
    @Transient
    val guid: String = ""
)
