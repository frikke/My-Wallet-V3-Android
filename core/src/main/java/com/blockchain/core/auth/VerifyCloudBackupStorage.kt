package com.blockchain.core.auth

import com.blockchain.store.CacheConfiguration
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.IsCachedMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import info.blockchain.wallet.api.WalletApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer

class VerifyCloudBackupStorage(private val walletApi: WalletApi) :
    KeyedStore<VerifyCloudCredentials, Unit> by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
        storeId = "VerifyCloudBackupStorage",
        reset = CacheConfiguration.onLogout(),
        keySerializer = VerifyCloudCredentials.serializer(),
        dataSerializer = Unit.serializer(),
        fetcher = Fetcher.Keyed.ofSingle { key ->
            walletApi.verifyCloudBackup(
                guid = key.walletGuid,
                sharedKey = key.sharedKey,
                deviceType = key.deviceType,
                hasCloudBackup = key.hasCloudBackup
            ).toSingle { }
        },
        mediator = IsCachedMediator()
    )

@Serializable
data class VerifyCloudCredentials(
    @Transient
    val walletGuid: String = "",
    @Transient
    val sharedKey: String = "",
    val hasCloudBackup: Boolean,
    val deviceType: Int
)
