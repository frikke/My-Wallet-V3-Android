package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.mapFromMetadata
import com.blockchain.nabu.models.responses.tokenresponse.mapToNabuAccountMetadata
import com.blockchain.nabu.models.responses.tokenresponse.mapToNabuUserMetadata
import com.blockchain.rx.maybeCache
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

@OptIn(InternalSerializationApi::class)
class MetadataRepositoryNabuTokenAdapter(
    private val metadataRepository: MetadataRepository,
    private val createNabuToken: CreateNabuToken,
    private val accountMetadataMigrationFF: FeatureFlag
) : NabuToken {

    private fun createMetaData(currency: String?, action: String?): Maybe<CredentialMetadata> = Maybe.defer {
        createNabuToken.createNabuOfflineToken(currency, action)
            .zipWith(accountMetadataMigrationFF.enabled)
            .map { (tokenResponse, enabled) ->
                if (enabled) {
                    tokenResponse.mapToNabuAccountMetadata()
                } else {
                    tokenResponse.mapToNabuUserMetadata()
                }
            }
            .flatMapMaybe { credentialsMetadata ->
                when (credentialsMetadata) {
                    is NabuAccountCredentialsMetadata -> {
                        metadataRepository.saveMetadata(
                            credentialsMetadata,
                            NabuAccountCredentialsMetadata::class.java,
                            NabuAccountCredentialsMetadata::class.serializer(),
                            NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE
                        ).andThen(Maybe.just(credentialsMetadata))
                    }
                    is NabuUserCredentialsMetadata -> {
                        metadataRepository.saveMetadata(
                            credentialsMetadata,
                            NabuUserCredentialsMetadata::class.java,
                            NabuUserCredentialsMetadata::class.serializer(),
                            NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
                        ).andThen(Maybe.just(credentialsMetadata))
                    }
                }
            }
    }

    private val defer = Maybe.defer {
        metadataRepository.loadMetadata(
            NabuAccountCredentialsMetadata.ACCOUNT_CREDENTIALS_METADATA_NODE,
            NabuAccountCredentialsMetadata::class.serializer(),
            NabuAccountCredentialsMetadata::class.java
        ).flatMap {
            if (it.isValid()) {
                return@flatMap Maybe.just(it)
            } else {
                metadataRepository.loadMetadata(
                    NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
                    NabuUserCredentialsMetadata::class.serializer(),
                    NabuUserCredentialsMetadata::class.java
                )
            }
        }.onErrorResumeNext {
            metadataRepository.loadMetadata(
                NabuUserCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
                NabuUserCredentialsMetadata::class.serializer(),
                NabuUserCredentialsMetadata::class.java
            )
        }
    }.maybeCache()
        .filter { it.isValid() }

    override fun fetchNabuToken(currency: String?, action: String?): Single<NabuOfflineTokenResponse> =
        defer
            .switchIfEmpty(createMetaData(currency, action))
            .map { metadata ->
                if (!metadata.isValid()) {
                    throw MetadataNotFoundException("Nabu Token is empty")
                }
                metadata.mapFromMetadata()
            }
            .toSingle()
}
