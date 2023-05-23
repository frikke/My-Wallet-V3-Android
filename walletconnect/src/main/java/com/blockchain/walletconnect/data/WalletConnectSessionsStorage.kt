package com.blockchain.walletconnect.data

import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class WalletConnectSessionsStorage(private val metadataRepository: MetadataRepository) :
    Store<WalletConnectMetadata>
    by PersistedJsonSqlDelightStoreBuilder().build(
        storeId = "WalletConnectSessionsStorage",
        fetcher = Fetcher.Keyed.ofSingle {

            metadataRepository.loadRawValue(MetadataEntry.WALLET_CONNECT_METADATA).map { json ->
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }.decodeFromString<WalletConnectMetadata>(json)
            }.switchIfEmpty(Single.just(WalletConnectMetadata(WalletConnectSessions(emptyList()))))
        },
        dataSerializer = WalletConnectMetadata.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    )
