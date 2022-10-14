package com.blockchain.nabu.metadata

import com.blockchain.logging.Logger
import com.blockchain.logging.RemoteLogger
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.load
import com.blockchain.metadata.save
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.models.responses.tokenresponse.mapToBlockchainCredentialsMetadata
import com.blockchain.utils.thenMaybe
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe

class AccountCredentialsMetadata(
    private val metadataRepository: MetadataRepository,
    private val remoteLogger: RemoteLogger
) {
    private var loadMetadataMaybe: Maybe<CredentialMetadata>? = null

    @Synchronized
    fun load(): Maybe<CredentialMetadata> {
        loadMetadataMaybe?.let {
            Logger.d("Metadata loading already")
            return it
        } ?: kotlin.run {
            return loadCached().doFinally {
                loadMetadataMaybe = null
            }.also {
                loadMetadataMaybe = it
            }
        }
    }

    private fun loadCached(): Maybe<CredentialMetadata> =
        metadataRepository.load<BlockchainAccountCredentialsMetadata>(
            MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS,
        ).reSyncIfNeeded()
            .switchIfEmpty(
                // Not found - haven't been created yet.
                Maybe.just(BlockchainAccountCredentialsMetadata.invalid())
            ).flatMap { blockchainMetadata ->
                if (blockchainMetadata.isValid()) {
                    Maybe.just(blockchainMetadata)
                } else {
                    metadataRepository.load<NabuLegacyCredentialsMetadata>(
                        MetadataEntry.NABU_LEGACY_CREDENTIALS,
                    ).reSyncLegacyIfNeeded().filter { it.isValid() }.flatMap { legacyMetadata ->
                        migrate(legacyMetadata, blockchainMetadata).thenMaybe {
                            Maybe.just(legacyMetadata)
                        }
                    }
                }
            }.cache()

    private fun Maybe<BlockchainAccountCredentialsMetadata>.reSyncIfNeeded():
        Maybe<BlockchainAccountCredentialsMetadata> {
        return flatMap {
            if (it.isCorrupted) {
                remoteLogger.logEvent("Syncing corrupted metadata entry 14")
                metadataRepository.save<BlockchainAccountCredentialsMetadata>(
                    it,
                    MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
                ).onErrorComplete().thenMaybe { Maybe.just(it) }
            } else
                Maybe.just(it)
        }
    }

    private fun Maybe<NabuLegacyCredentialsMetadata>.reSyncLegacyIfNeeded(): Maybe<NabuLegacyCredentialsMetadata> {
        return flatMap {
            if (it.isCorrupted) {
                remoteLogger.logEvent("Syncing corrupted metadata entry 10")
                metadataRepository.save<NabuLegacyCredentialsMetadata>(
                    it,
                    MetadataEntry.NABU_LEGACY_CREDENTIALS
                ).onErrorComplete().thenMaybe { Maybe.just(it) }
            } else
                Maybe.just(it)
        }
    }

    fun save(tokenResponse: NabuOfflineToken): Completable {
        return saveMetadata(tokenResponse)
    }

    private fun saveMetadata(tokenResponse: NabuOfflineToken): Completable {
        val metadata = tokenResponse.mapToBlockchainCredentialsMetadata()
        return metadataRepository.save(
            metadata,
            MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
        )
    }

    private fun migrate(
        legacyMetadata: NabuLegacyCredentialsMetadata,
        blockchainMetadata: BlockchainAccountCredentialsMetadata
    ): Completable {
        return metadataRepository.save(
            BlockchainAccountCredentialsMetadata(
                userId = legacyMetadata.userId,
                exchangeLifetimeToken = blockchainMetadata.exchangeLifetimeToken?.takeIf { it.isNotEmpty() },
                lifetimeToken = legacyMetadata.lifetimeToken,
                exchangeUserId = blockchainMetadata.exchangeUserId?.takeIf { it.isNotEmpty() }
            ),
            MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
        )
    }
}
